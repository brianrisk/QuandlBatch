import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


/**
 * 
 * 
 * Features:
 *		daily updates data
 *		only downloads the data that is new since the last last download
 *		once per day downloads constituent lists to monitor added or removed constituents
 *		performs full download of newly added constituents
 *		removes data files that are no longer constituents
 *		Programmatically checks speed limit
 *		creates log file of non-200 response codes
 *		immediately begins download when program launched
 *		handles interrupted downloads
 *		no downloads on weekends
 *		sleeps when not in use
 *
 * Future:
 * 		tracks data set download times and restarts if taking too long (e.g. download interrupted due to loss of internet or computer sleeping)
 * 		queues for multiple databases
 * 		let's user set hour of download
 * 		creates log file of download completion
 * 		Work with any Quandl database
 *
 * Notes:
 *		http response code: 200 = OK, 429 = too fast
 *
 * @author brianrisk
 * @see <a href="http://github.com/brianrisk/QuandlDownloader">Github repository</a>
 *
 */

public class QuandlBatch {

	// focusing on EOD for this release
	private static String dbName = "EOD";

	// time constants
	private static final long SECOND = 1000;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = MINUTE * 60;
	private static final long DAY = HOUR * 24;

	// how dates are formatted
	static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	// the hour of the day
	private static int theHour = -1;

	// time in milliseconds when the last download was initiated.
	private static long downloadInitiatedTime = 0;

	// list of data sets we are downloading
	private static ArrayList<String> constituents;

	// storage locations
	private static File downloadsFolder;
	private static File constituentsFolder;
	private static File partialsFolder;

	public static void main(String [] args) {

		U.p("Welcome to QuandlDownloader");

		init();

		// loop forever
		while (true) {

			// getting the current hour of the day
			Date date = new Date();
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(date); 
			theHour = calendar.get(Calendar.HOUR_OF_DAY);

			// milliseconds since our last download
			long now = System.currentTimeMillis();
			long timeSinceLastDownload = now - downloadInitiatedTime;

			// making the boolean to perform download a bit easier to follow
			boolean performDownload = false;

			// if it's the hour for download (6PM) and we haven't recently done a download
			if (timeSinceLastDownload > HOUR && theHour == Settings.refreshHour ) performDownload = true;

			// if it's been more than a day since the download
			if (timeSinceLastDownload > DAY) performDownload = true;

			// if the program just started, perform a download
			if (downloadInitiatedTime == 0) performDownload = true;

			// don't download on weekends
			if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) performDownload = false;
			if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) performDownload = false;

			// download or sleep
			if (performDownload) {
				initiate();

				// folder for this database
				File dbFolder = new File(downloadsFolder, dbName);
				dbFolder.mkdir();

				// making a hashtable for easy reference
				Hashtable<String, String> constituentsHash = new Hashtable<String, String>();
				for (String constituent: constituents) {
					constituentsHash.put(constituent, constituent);
				}

				// delete data sets no longer in constituents file
				File [] existingFiles = dbFolder.listFiles();
				if (existingFiles != null) {
					for (File dataFile: existingFiles) {
						String name = U.getFileNameWithoutSuffix(dataFile);
						if (constituentsHash.get(name) == null) {
							dataFile.delete();
						}
					}
				}

				// download daily update file (partials)
				String partialUrl = "https://quandl.com/api/v3/databases/EOD/download?download_type=partial&auth_token=" + Settings.apiKey;
				File partialZip = new File(partialsFolder, dbName + ".zip");
				U.downloadFileFromURL(partialUrl, partialZip);
				File partialCsv = new File(partialsFolder, dbName + ".csv");
				U.unzip(partialZip, partialsFolder, partialCsv.getName());

				// download, monitor http response codes
				try {

					// load the updates into hashtable
					Hashtable<String, StockDay> partials = new Hashtable<String, StockDay>();
					BufferedReader partialReader = new BufferedReader(new FileReader(partialCsv));
					String partialLine = partialReader.readLine();
					partialLine = partialReader.readLine();
					while (partialLine != null) {
						int firstComma = partialLine.indexOf(',');
						String constituent = partialLine.substring(0, firstComma);
						String partialData = partialLine.substring(firstComma + 1);
						StockDay stockDay = new StockDay(partialData);
						if (stockDay.isValid) partials.put(constituent, stockDay);
						partialLine = partialReader.readLine();
					}
					partialReader.close();

					// error log file
					PrintWriter errors = new PrintWriter(new FileWriter("errors.txt"));

					// this list will hold constituents that encountered errors
					ArrayList<String> constituentsToRetry = new ArrayList<String>();
					for (String constituent: constituents) {
						StockDay latest = partials.get(constituent);
						int responseCode = download(constituent, latest);		
						// was downloading too quickly, pause for a bit and try again
						while (responseCode == 429) {
							responseCode = download(constituent, latest);
							U.sleep(10 * SECOND);
						}		
						if (responseCode != 200) {
							constituentsToRetry.add(constituent);
						}
					}
					// retry the constituents that didn't work the first time around
					// if they don't work again, log it
					for (String constituent: constituentsToRetry) {
						StockDay latest = partials.get(constituent);
						int responseCode = download(constituent, latest);		
						// was downloading too quickly, pause for a bit and try again
						while (responseCode == 429) {
							responseCode = download(constituent, latest);
							U.sleep(10 * SECOND);
						}		
						if (responseCode != 200 && responseCode != 0) {
							errors.println(responseCode + "\t" + constituent);
						}
					}
					errors.flush();
					errors.close();
				} catch (IOException e) {
					e.printStackTrace();
				}	
				U.p("download complete");
			} else {
				// sleep one minute
				U.sleep(MINUTE);
			}
		}
	}


	/**
	 * fully download data sets that are not yet downloaded
	 * for data sets that do exist, download only data needed and properly append
	 * 
	 * @param constituent
	 * @return http response code
	 */
	public static int download(String constituent, StockDay latest) {
		// the code we return
		int responseCode = 0;

		// setting up output destinations
		File dbFolder = new File(downloadsFolder, dbName);
		dbFolder.mkdirs();
		File dataFile = new File(dbFolder, constituent + ".csv");

		try {
			String urlString = "https://www.quandl.com/api/v1/datasets/" + dbName + "/" + constituent + ".csv?auth_token=" + Settings.apiKey;

			// if not exists, or a split happened today, downloading full data
			if (!dataFile.exists() || (latest != null && latest.split != 1)) {
				U.p("downloading " + constituent);
				PrintWriter pw = new PrintWriter(new FileWriter(dataFile));
				URL url = new URL(urlString);
				HttpURLConnection http = (HttpURLConnection)url.openConnection();
				responseCode = http.getResponseCode();
				BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
				String line = in.readLine();
				while (line != null) {
					pw.println(line);
					line = in.readLine();
				}
				pw.flush();
				pw.close();


			} 
			// else, downloading only data we need
			else {
				// load existing file
				BufferedReader br = new BufferedReader(new FileReader(dataFile));

				//read header
				String header = br.readLine();

				// load data set
				boolean successfullyLoadedData = true;
				Hashtable<Date, StockDay> StockDays = new Hashtable<Date, StockDay> ();
				String line = br.readLine();
				while (line != null) {
					StockDay stockDay = new StockDay(line);
					if (stockDay.isValid) {
						StockDays.put(stockDay.date, stockDay);
						line = br.readLine();
					} else {
						U.p(line);
						successfullyLoadedData = false;
						break;
					}
				}
				br.close();

				if (StockDays.size() == 0) successfullyLoadedData = false;

				if (successfullyLoadedData) {
					// find last day
					ArrayList<StockDay> stockDayList = new ArrayList<StockDay>(StockDays.values());
					Collections.sort(stockDayList);
					StockDay latestDay = stockDayList.get(0);

					// if last day is not today, no loading needs to happen
					Date today = new Date(System.currentTimeMillis());
					boolean lastDataPointIsNotToday = !U.isSameDay(latestDay.date, today);


					if (lastDataPointIsNotToday) {

						// if last data day is Friday, and this is Monday
						boolean isAfterWeekend = false;
						if (latest != null) {
							Calendar calendar = GregorianCalendar.getInstance();
							calendar.setTime(latest.date); 
							int lastDownloadDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
							calendar.setTime(new Date());
							int todayDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
							isAfterWeekend = (todayDayOfWeek == Calendar.MONDAY && lastDownloadDayOfWeek == Calendar.FRIDAY);
						}

						// if last data day is yesterday
						boolean lastDayIsYesterday = false;
						if (latest != null) lastDayIsYesterday = U.isYesterday(latest.date);

						// checking if all we need to do is add in the data from the partial
						if ((latest != null) && (isAfterWeekend || lastDayIsYesterday)) {
							StockDays.put(latest.date, latest);
						}
						// perform customized download of selected date range
						else {
							U.p("downloading " + constituent);
							String start = dateFormat.format(latestDay.date);
							String stop = dateFormat.format(today);
							urlString += "&trim_start=" + start + "&trim_end=" + stop + "&exclude_headers=true"; 
							URL url = new URL(urlString);
							HttpURLConnection http = (HttpURLConnection)url.openConnection();
							responseCode = http.getResponseCode();
							// download, add new data to hashtable
							BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
							line = in.readLine();
							while (line != null) {
								StockDay stockDay = new StockDay(line);
								if (stockDay.isValid) StockDays.put(stockDay.date, stockDay);
								line = in.readLine();
							}
						}

						// sort list
						stockDayList = new ArrayList<StockDay>(StockDays.values());
						Collections.sort(stockDayList);

						// write header and sorted list to out file
						PrintWriter pw = new PrintWriter(new FileWriter(dataFile));
						pw.println(header);
						for (StockDay stockDay: stockDayList) {
							pw.println(stockDay);
						}
						pw.flush();
						pw.close();		
					}


				}
				// if data was NOT successfully loaded
				else {
					// delete the file
					dataFile.delete();
					U.p("deleted potentially corrupted file for " + constituent);
					// re-call this method
					download(constituent, latest);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responseCode;
	}


	/**
	 * Load settings, set up folders
	 */
	public static void init() {
		Settings.load();
		downloadsFolder = new File("downloads");
		downloadsFolder.mkdirs();
		constituentsFolder = new File("constituents");
		constituentsFolder.mkdirs();
		partialsFolder = new File("partials");
		partialsFolder.mkdirs();
	}


	/**
	 * Called when we are beginning a bulk download
	 * Gets a fresh copy of the constituents
	 * creates appropriate directories
	 */
	public static void initiate() {

		downloadInitiatedTime = System.currentTimeMillis();

		// download our constituents list
		String constituentsUrl = "http://static.quandl.com/end_of_day_us_stocks/ticker_list.csv";
		File constituentsFile = new File(constituentsFolder, dbName + ".csv");
		U.downloadFileFromURL(constituentsUrl, constituentsFile);

		// load the constituents file
		constituents =  new ArrayList<String>();
		try {
			Reader in = new FileReader(constituentsFile);
			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
			int rowIndex = 0;
			for (CSVRecord record : records) {
				//skipping the header row
				if (rowIndex != 0) {
					String constituent = record.get(0);
					constituent = constituent.replace('.', '_');
					constituents.add(constituent);
				}
				rowIndex++;
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}





}
