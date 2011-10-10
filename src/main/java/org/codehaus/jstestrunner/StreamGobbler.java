package org.codehaus.jstestrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * StreamGobbler; read from an InputStream in it's own thread, either writing to an
 * output stream with a TYPE marker, or discarding.
 * 
 * Reference: http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
 * 
 * @author Ben Jones
 */
class StreamGobbler extends Thread {
	InputStream inputStream;
	OutputStream outputStream;

	/**
	 * Construct StreamGobbler which discards input.
	 * @param inputStream InputStream to read from.
	 */
	StreamGobbler(InputStream inputStream) {
		this(inputStream, null);
	}

	/**
	 * Construct StreamGobbler to write to an OutputStream.
	 * @param inputStream InputStream to read from.
	 * @param outputStream OutputStream to send the InputStream to.
	 */
	StreamGobbler(InputStream inputStream, OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	/**
	 * Thread.run method which will 'gobble' all input from the input stream and
	 * either write it to the OutputStream the object was initialized with, or 
	 * discard it if no OutputStream was supplied.
	 */
	public void run() {
		// Setup a PrintWriter if we have an OutputStream
		PrintWriter printWriter = null;
		if (outputStream != null) {
			printWriter = new PrintWriter(outputStream);
		}
		// Setup input stream buffered reader
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		
		try {
			// Read all lines from the input stream
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				// If we have an output writer, write each line to it
				if (printWriter != null) {
					printWriter.println(line);
				}
			}			
			
		} catch (IOException ioe) {
			// On IO error reading the input stream, print the stack trace
			ioe.printStackTrace();
			
		} finally {
			// Close the input reader
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch(IOException ignore) {}
			}
		}

		// If we have an output writer, flush and close it
		if (printWriter != null) {
			printWriter.flush();
			printWriter.close();
		}
	}
	
}