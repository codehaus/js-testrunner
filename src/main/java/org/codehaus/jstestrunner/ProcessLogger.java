package org.codehaus.jstestrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProcessLogger. Logs all output from a process (STDOUT+STDERR) to the class
 * logger, and also attempts to log the process return code.
 * 
 * @author Ben Jones
 */
class ProcessLogger extends Thread {
	private static Logger logger = Logger.getLogger(ProcessLogger.class.getName());
	private Process process;

	/**
	 * Construct a ProcessLogger.
	 * @param process The process to log the output for.
	 */
	ProcessLogger(Process process) {
		this.process = process;
	}

	/**
	 * Thread.run method which reads all output from the process (from getInputStream() only),
	 * logs this to a file, and when complete attempts to log the exit code of the process.
	 */
	public void run() {		
		InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		
		try {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				logger.log(Level.FINE, line);
			}			
			
		} catch (IOException ioe) {
			logger.log(Level.WARNING, ioe.getLocalizedMessage());
			
		} finally {
			// Close the input reader
			try {
				bufferedReader.close();
			} catch(IOException ioe) {
				logger.log(Level.WARNING, ioe.getLocalizedMessage());
			}
		}
		
		try {
			int exitVal = process.waitFor();
			logger.log(Level.FINE, "Process exitValue: " + exitVal);
		} catch (InterruptedException e) {
			logger.log(Level.WARNING,
					"Problem waiting for completion." + e.toString());
		}
	}
	
}