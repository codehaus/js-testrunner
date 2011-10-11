/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

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
			logger.log(Level.WARNING, e.toString());
			
		} finally {
			// Close the input reader
			try {
				bufferedReader.close();
			} catch(IOException e) {
				logger.log(Level.WARNING, e.toString());
			}
		}
		
		// If we have a process logger, attempt to determine and log the exit code
		if (processLogger != null) {
			logExitValue(process, processLogger);
		}
	}
	
	/**
	 * Attempt to log the exit value of the process to the supplied logger.
	 * Will block until the process exits.
	 * @param process The process to log the exit value for. 
	 * @param processLogger The logger to log to.
	 */
	public static void logExitValue(Process process, Logger processLogger) {
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