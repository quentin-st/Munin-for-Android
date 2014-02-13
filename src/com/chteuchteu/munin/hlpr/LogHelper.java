package com.chteuchteu.munin.hlpr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

/**
 * This class may be used if we need to add some log features
 * We should log information about non-fatal errors and add some option
 * to send this to an adress mail
 */
public final class LogHelper {
	public static void log(String text) {
		File logFile = new File(Environment.getExternalStorageDirectory().toString(), "/muninForAndroid/log.txt");
		if(!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		String output = Util.Dates.getNow() + "\t " + text;
		
		try {
			BufferedWriter outputBw = new BufferedWriter(new FileWriter(logFile));
			outputBw.write(output);
			outputBw.close();
		}
		catch (IOException ex) { Log.e("", ex.toString()); }
		catch (Exception ex) { Log.e("", ex.toString()); }
	}
	
	public static String getLog() {
		File logFile = new File(Environment.getExternalStorageDirectory().toString(), "/muninForAndroid/log.txt");
		String text = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(logFile));
			String line;
			
			while ((line = br.readLine()) != null)
				line += line + "\n";
			
			br.close();
		}
		catch (IOException e) { }
		
		return text;
	}
	
	public static void recycleLog() {
		// TODO
	}
}