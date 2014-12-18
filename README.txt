QuandlDownload

Author:  Brian Risk
Dontact: brian@geneffects.com
License:  https://creativecommons.org/licenses/by/3.0/us/

WHAT IT DOES
	QuandlDownload is a bulk download utility for Quandl Data.

REQUIREMENTS
	JRE 1.6 or higher

SETTINGS
	duplicate "settings-sample.txt" and change the name to "settings.txt".
	Edit "settings.txt" and add your API key to the "apiKey" variable where
	the existing value is "xxxxxx". 

RUNNING
From command line:
	java -jar QuandlDownload
	
ERRORS
	"No settings.txt file detected" -- follow the steps in SETTINGS
	"Invalid API key" -- make sure you have a valid apiKey value in "settings.txt"
