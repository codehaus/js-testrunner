package org.codehaus.jstestrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProcessLogger; handles output from a process and either discards it or
 * logs it to the supplied logger. This is used to interact with the test runner
 * processes.
 * 
 * @author Ben Jones
 */
class ProcessLogger extends Thread {
	private static Logger logger = Logger.getLogger(ProcessLogger.class.getName());
	private Logger processLogger;
	private Process process;

	/**
	 * Construct a ProcessLogger to handle output from a process. If a process logger is
	 * provided then information is logged to that logger. If it is not provided, then
	 * output is discarded.
	 * @param process The process to handle output for.
	 * @param processLogger (optional) The logger to log messages to. If null, then the
	 * input stream will be read and its contents discarded.
	 */
	ProcessLogger(Process process, Logger processLogger) {
		this.process = process;
		this.processLogger = processLogger;
	}

	/**
	 * Thread.run method which causes the ProcessLogger to read all lines from 
	 * process.getInputStream() and either log (if a process logger was provided)
	 * or discard them. If a process logger was provided, it also attempts to 
	 * determine the process exit code and log that.
	 */
	public void run() {		
		InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		
		try {
			String line;
			// Read every line of input
			while ((line = bufferedReader.readLine()) != null) {
				// If we have a process logger, log each line. Otherwise discard.
				if (processLogger != null) {
					processLogger.log(Level.FINE, line);
				}
			}			
			
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getLocalizedMessage());
			
		} finally {
			// Close the input reader
			try {
				bufferedReader.close();
			} catch(IOException e) {
				logger.log(Level.WARNING, e.getLocalizedMessage());
			}
		}
		
		// If we have a process logger, attempt to determine and log the exit code
		if (processLogger != null) {
			try {
				int exitVal = process.waitFor();
				processLogger.log(Level.FINE, "Process exitValue: " + exitVal);
			} catch (InterruptedException e) {
				processLogger.log(Level.WARNING,
						"Problem waiting for completion." + e.toString());
			}
		}
	}
	
}