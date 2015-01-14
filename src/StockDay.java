import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class StockDay implements Comparable<StockDay>{

	static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	String line;
	boolean isValid;

	Date date;
	double open;
	double high;
	double low;
	double close;
	double volume = 0;
	double dividend;
	double split;
	double adj_open;
	double adj_high;
	double adj_low;
	double adj_close;
	double adj_volume = 0;

	/**
	 * values based on content of line taken from EOD csv file
	 * 
	 * Date,Open,High,Low,Close,Volume,Dividend,Split,Adj_Open,Adj_High,Adj_Low,Adj_Close,Adj_Volume
	 * 
	 * @param line
	 */
	public StockDay(String line) {
		this.line = line;
		isValid = true;
		String [] chunks = line.split(",");
		if (chunks.length == 13) {

			for (String chunk: chunks) {
				//if (chunk.equals("")) isValid = false;
			}

			if (isValid) {
				try {
					date = dateFormat.parse(chunks[0]);
				} catch (ParseException e) {
					isValid = false;	
				}

				try {
					open = Double.parseDouble(chunks[1]);
					high = Double.parseDouble(chunks[2]);
					low = Double.parseDouble(chunks[3]);
					close = Double.parseDouble(chunks[4]);
					volume = Double.parseDouble(chunks[5]);
					dividend = Double.parseDouble(chunks[6]);
					split = Double.parseDouble(chunks[7]);
					adj_open = Double.parseDouble(chunks[8]);
					adj_high = Double.parseDouble(chunks[9]);
					adj_low = Double.parseDouble(chunks[10]);
					adj_close = Double.parseDouble(chunks[11]);
					adj_volume = Double.parseDouble(chunks[12]);
				} catch (NumberFormatException nfe){
					//isValid = false;
				}
			}
		} else {
			isValid = false;
		}

	}

	public String toString() {
		return line;
	}

	@Override
	public int compareTo(StockDay other) {
		return -1 * date.compareTo(other.date);
	}

}
