package redstonelamp.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.ConcurrentModificationException;

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import redstonelamp.RedstoneLamp;
import redstonelamp.Server;

public class MainLogger {
	private PrintWriter writer;
	private Collection<String> lines = new ArrayList<String>();
	
	public MainLogger() {}
	
	/**
	 * A normal Server Message
	 * 
	 * @param String
	 */
	public void info(String message) {
		message = TextFormat.stripColors(message);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		message = sdf.format(cal.getTime()) + " [INFO] " + message;
		writeToLog(message);
		System.out.println(message);
	}
	
	/**
	 * Used for debuging things<br>
	 * This will only appear if debug mode is enabled in the server.properties
	 * 
	 * @param String
	 */
	public void debug(String message) {
		message = TextFormat.stripColors(message);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		message = sdf.format(cal.getTime()) + " [DEBUG] " + message;
		if(RedstoneLamp.getServerInstance() != null && RedstoneLamp.getServerInstance().isDebugMode())
			System.out.println(message);
		else if(Boolean.parseBoolean(((String)RedstoneLamp.yaml.getInMap("debug").get("enabled"))))
			System.out.println(message);
		writeToLog(message);
	}
	
	/**
	 * Warns the server of a minor issue
	 * 
	 * @param String
	 */
	public void warn(String message) {
		message = TextFormat.stripColors(message);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		message = sdf.format(cal.getTime()) + " [WARNING] " + message;
		System.out.println(message);
		writeToLog(message);
	}
	
	/**
	 * Warns the server of a minor issue
	 * 
	 * @param String
	 */
	public void warning(String message) {
		message = TextFormat.stripColors(message);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		message = sdf.format(cal.getTime()) + " [WARNING] " + message;
		System.out.println(message);
		writeToLog(message);
	}
	
	/**
	 * Shows an issue to the server that may cause problems
	 * 
	 * @param String
	 */
	public void error(String message) {
		message = TextFormat.stripColors(message);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		message = sdf.format(cal.getTime()) + " [ERROR] " + message;
		System.out.println(message);
		writeToLog(message);
	}
	
	/**
	 * Shows a message right before a server crash
	 * 
	 * @param String
	 */
	public void fatal(String message) {
		message = TextFormat.stripColors(message);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		message = sdf.format(cal.getTime()) + " [FATAL] " + message;
		System.out.println(message);
		writeToLog(message);
		if(RedstoneLamp.getServerInstance() instanceof Server)
			RedstoneLamp.getServerInstance().stop();
		else
			System.exit(1);
	}

	public void noTag(String message) {
		message = TextFormat.stripColors(message);
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		System.out.println(sdf.format(cal.getTime()) + " " + message);
	}
	
	public void writeToLog(String string) {
		string = TextFormat.stripColors(string);
		lines.add(string);
	}
	
	public void close() {
		Calendar cal = Calendar.getInstance();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("mmddyyyyHHmmss");
			File logsDir = new File("logs/");
			File log = new File(logsDir + "/" + sdf.format(cal.getTime()) + ".log");
			if(!logsDir.isDirectory())
				logsDir.mkdirs();
			FileUtils.writeLines(log, lines);
		} catch (IOException e) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			System.out.println(sdf.format(cal.getTime()) + " [ERROR] Unable to write log!");
		} catch(ConcurrentModificationException e){
			System.out.println("Failed to write log! "+e.getClass().getName()+": "+e.getMessage());
		}
	}
}