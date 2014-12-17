import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class Settings {
	
	// API key allowing access to data
	public static String apiKey = null;
	
	//the hour of the day (0 to 23) to begin the data refresh
	public static Integer refreshHour = 18;
	
	static {
		load();
	}
	
	
	public static void load() {
		
		File settingsFile = new File("settings.txt");
		if (!settingsFile.exists()) {
			U.fail("no settings file found at: " + settingsFile.getPath());;
		}
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(settingsFile));
			String line = br.readLine();
			
			while (line != null) {
				line = line.trim();
				if (!line.startsWith("//") && !line.isEmpty()) {
					String [] chunks = line.split("\t");
					if (chunks.length == 2) {
						String key = chunks[0];
						String value = chunks[1];
						if (key.equals("apiKey")) apiKey = value;
					}
				}
				line = br.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
