import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utilities
 * 
 * @author brianrisk
 *
 */
public class U {

	public static void fail(String s) {
		p(s);
		System.exit(1);
	}

	public static void p(Object o) {
		System.out.println(o);
	}

	public static String stripPunctuation(String line) {
		// from:
		// http://stackoverflow.com/questions/17531362/efficiently-removing-specific-characters-some-punctuation-from-strings-in-java
		return line.replaceAll("\\p{Punct}+", "");
	}

	/**
	 * http://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-a-numeric-type-in-java
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str)  {  
		try {  
			Double.parseDouble(str);  
		} catch(NumberFormatException nfe)  {  
			return false;  
		}  
		return true;  
	}

	public static boolean hasWord(String haystack, String needle) {
		String haystackAlterdCase = haystack.toLowerCase();
		String needleAlteredCase = needle.toLowerCase();
		if (haystackAlterdCase.matches(".*\\b" + needleAlteredCase + "\\b.*")) return true;
		//		if (haystackAlterdCase.startsWith(needleAlteredCase)) return true;
		//		if (haystackAlterdCase.endsWith(needleAlteredCase)) return true;
		return false;
	}

	/**
	 * Utility file to download a file from a URL
	 * 
	 * @param urlString
	 * @param destination
	 */
	public static void downloadFileFromURL(String urlString, File destination) {	
		try {
			URL url = new URL(urlString);
			HttpURLConnection http = (HttpURLConnection)url.openConnection();
			http.setInstanceFollowRedirects( false );
			http.connect(); 
			int responseCode = http.getResponseCode();
			// loop until no longer redirection
			while ((responseCode / 100) == 3) {
				String newLocationHeader = http.getHeaderField( "Location" );
				url = new URL(newLocationHeader);
				http = (HttpURLConnection)url.openConnection();
				responseCode = http.getResponseCode();
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream()));
			BufferedWriter out = new BufferedWriter(new FileWriter(destination));
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read()) != -1) {
				out.write(read);
			}
			out.flush();
			out.close();
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * unzips - assumes there is one file in the zip
	 * Based on:
	 * http://www.mkyong.com/java/how-to-decompress-files-from-a-zip-file/
	 * 
	 * @param zipFile input zip file
	 * @param output zip file output folder
	 */
	public static void unzip(File zipFile, File outputFolder, String fileName){

		byte[] buffer = new byte[1024];

		try{
			//get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			//get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while(ze!=null){

//				String fileName = ze.getName();
				File newFile = new File(outputFolder, fileName);

				System.out.println("file unzip : "+ newFile.getAbsoluteFile());

				//create all non exists folders
				//else you will hit FileNotFoundException for compressed folder
				new File(newFile.getParent()).mkdirs();

				FileOutputStream fos = new FileOutputStream(newFile);             

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();   
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();

			System.out.println("Done");

		}catch(IOException ex){
			ex.printStackTrace(); 
		}
	}    


	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}


	public static String getFileNameWithoutSuffix(File file) {
		return file.getName().substring(0, file.getName().lastIndexOf('.'));
	}


	/**
	 * returns true if two dates occur on same day
	 * from: http://stackoverflow.com/questions/2517709/comparing-two-dates-to-see-if-they-are-in-the-same-day
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static boolean isSameDay(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);
		boolean sameDay = cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
				cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
		return sameDay;
	}


	/**
	 * checks if a date is from yesterday
	 * adapted from: http://stackoverflow.com/questions/3006150/how-to-check-if-a-date-object-equals-yesterday
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static boolean isYesterday(Date possibleYesterdayDate) {

		Calendar yesterday = Calendar.getInstance();
		yesterday.add(Calendar.DAY_OF_YEAR, -1);

		Calendar possibleYesterday = Calendar.getInstance();
		possibleYesterday.setTime(possibleYesterdayDate);

		boolean isYesterday = (yesterday.get(Calendar.YEAR) == possibleYesterday.get(Calendar.YEAR)
				&& yesterday.get(Calendar.DAY_OF_YEAR) == possibleYesterday.get(Calendar.DAY_OF_YEAR));

		return isYesterday;
	}

}
