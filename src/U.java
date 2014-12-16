import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Calendar;
import java.util.Date;

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
	 * Based on:
	 * http://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
	 * @param urlString
	 * @param destination
	 */
	public static void downloadFileFromURL(String urlString, File destination) {	
		try {
			URL website = new URL(urlString);
			ReadableByteChannel rbc;
			rbc = Channels.newChannel(website.openStream());
			FileOutputStream fos = new FileOutputStream(destination);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
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

}
