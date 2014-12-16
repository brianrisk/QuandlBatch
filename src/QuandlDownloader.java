import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
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
 * Features:
 * 		regularly updates
 * 		only downloads the data that is new since the last last download
 * 		once per day downloads constituent lists to monitor added or removed constituents
 *		performs full download of newly added constituents
 *		removes data files that are no longer constituents
 * 		manages download speed limit (2000 calls / 10 minutes)
 *		immediately begins download when program launched
 *		no downloads on weekends
 *		sleeps when not in use
 *
 * Future:
 * 		queues for multiple databases
 * 		Programmatically checks speed limit
 * 		let's user set hour of download
 * 		handle interrupted downloads
 * 		creates log file of download completion
 * 		creates log file of errors and warnings
 *
 * Notes:
 *		http code: 200 = OK, 429 = too fast
 *
 * @author brianrisk
 * @see <a href="http://github.com/brianrisk/QuandlDownloader">Github repository</a>
 *
 */

public class QuandlDownloader {

	// focusing on EOD for this release
	private static String dbName = "EOD";

	// time constants
	private static final long SECOND = 1000;
	private static final long MINUTE = 60 * SECOND;
	private static final long HOUR = MINUTE * 60;
	
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

	public static void main(String [] args) {

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
			
			// if it's 6PM and we haven't recently done a download
			if (timeSinceLastDownload > HOUR && theHour == 18 ) performDownload = true;
			
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
				if (!dbFolder.exists()) dbFolder.mkdir();

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

				// download while keeping within speed limit
				long minuteStart = System.currentTimeMillis();
				long minuteEnd = minuteStart + MINUTE;
				int minuteCount = 0;
				for (String constituent: constituents) {
					if (minuteCount < Settings.speedLimit) {
						download(constituent);
						minuteCount++;
					} else {
						now = System.currentTimeMillis();
						while (now < minuteEnd) {
							U.sleep(SECOND);
							now = System.currentTimeMillis();
						}
						minuteStart = System.currentTimeMillis();
						minuteEnd = minuteStart + MINUTE;
						minuteCount = 0;
						
					}
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
	 */
	public static void download(String constituent) {
		U.p("downloading " + constituent);
		
		File dbFolder = new File(downloadsFolder, dbName);
		dbFolder.mkdirs();
		File dataFile = new File(dbFolder, constituent + ".csv");
		
		
		try {
			String urlString = "http://www.quandl.com/api/v1/datasets/" + dbName + "/" + constituent + ".csv?auth_token=" + Settings.apiKey;
			
			// if not exists, downloading full data
			if (!dataFile.exists()) {
				PrintWriter pw = new PrintWriter(new FileWriter(dataFile));
				URL url = new URL(urlString);
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String line = in.readLine();
				while (line != null) {
					pw.println(line);
					line = in.readLine();
				}
				pw.flush();
				pw.close();
			
			// else, downloading only data we need
			} else {
				// load existing file
				BufferedReader br = new BufferedReader(new FileReader(dataFile));
				
				//read header
				String header = br.readLine();
				
				// load data set
				Hashtable<Date, DataRow> dataRows = new Hashtable<Date, DataRow> ();
				String line = br.readLine();
				while (line != null) {
					DataRow dataRow = new DataRow(line);
					dataRows.put(dataRow.date, dataRow);
					line = br.readLine();
				}
				br.close();
				
				// find last day
				ArrayList<DataRow> dataRowList = new ArrayList<DataRow>(dataRows.values());
				Collections.sort(dataRowList);
				DataRow latestDay = dataRowList.get(0);
				
				// if last day is not today, load from last day to today
				Date today = new Date(System.currentTimeMillis());
				boolean lastDataPointIsNotToday = !U.isSameDay(latestDay.date, today);
				if (lastDataPointIsNotToday && theHour >= Settings.refreshHour) {
					String start = dateFormat.format(latestDay.date);
					String stop = dateFormat.format(today);
					// add new data to list
					urlString += "&trim_start=" + start + "&trim_end=" + stop; 
					URL url = new URL(urlString);
					BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
					// two readLine calls to skip the header
					line = in.readLine();
					line = in.readLine();
					while (line != null) {
						DataRow dataRow = new DataRow(line);
						dataRows.put(dataRow.date, dataRow);
						line = in.readLine();
					}
					
					// sort list
					dataRowList = new ArrayList<DataRow>(dataRows.values());
					Collections.sort(dataRowList);
					
					// write header and sorted list to out file
					PrintWriter pw = new PrintWriter(new FileWriter(dataFile));
					pw.println(header);
					for (DataRow dataRow: dataRowList) {
						pw.println(dataRow);
					}
					pw.flush();
					pw.close();		
				}	
				
			}
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	/**
	 * Load settings, set up folders
	 */
	public static void init() {
		Settings.load();
		downloadsFolder = new File("downloads");
		constituentsFolder = new File("constituents");
		constituentsFolder.mkdirs();
	}


	/**
	 * Called when we are beginning a bulk download
	 * Gets a fresh copy of the constituents
	 * creates appropriate directories
	 */
	public static void initiate() {

		// download our constituents list
		String constituentsUrl = "http://static.quandl.com/end_of_day_us_stocks/ticker_list.csv";
		File constituentsFile = downloadConstituents(constituentsUrl, dbName);

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


	/**
	 * Gets the components file for the S&P500
	 * Other indices and full exchange listings can be found here:
	 * https://www.quandl.com/resources/useful-lists
	 */
	public static File downloadConstituents(String url, String dbName) {
		File constituentsFile = new File(constituentsFolder, dbName + ".csv");
		U.downloadFileFromURL(url, constituentsFile);
		return constituentsFile;
	}
	

}
