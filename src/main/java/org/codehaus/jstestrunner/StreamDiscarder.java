package org.codehaus.jstestrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * StreamDiscarder; read from an InputStream in its own thread and discard.
 * 
 * @author Ben Jones
 */
class StreamDiscarder extends Thread {
	InputStream inputStream;

	/**
	 * Construct StreamDiscarder which discards input.
	 * @param inputStream InputStream to read from.
	 */
	StreamDiscarder(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	/**
	 * Thread.run method which will read all input from the input stream and
	 * discard it.
	 */
	public void run() {
		// Setup input stream buffered reader
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		
		try {
			// Read all lines from the input stream and do nothing with them
			while (bufferedReader.readLine() != null) {}			
			
		} catch (IOException ioe) {
			// On IO error reading the input stream, print the stack trace
			ioe.printStackTrace();
			
		} finally {
			// Close the input reader
			try {
				bufferedReader.close();
			} catch(IOException ignore) {}
		}
	}
	
}