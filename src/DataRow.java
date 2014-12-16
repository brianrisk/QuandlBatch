import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class DataRow implements Comparable<DataRow>{
	
	static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	Date date;
	String row;
	

	public DataRow(String row) {
		int firstComma = row.indexOf(',');
		String dateString = row.substring(0, firstComma);
		this.row = row;
		try {
			date = dateFormat.parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}
	
	public String toString() {
		return row;
	}
	

	/**
	 * Sorts descending
	 */
	@Override
	public int compareTo(DataRow other) {
		return -1 * date.compareTo(other.date);
	}

}
