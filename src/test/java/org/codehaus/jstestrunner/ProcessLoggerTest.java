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

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

/**
 * Tests for the ProcessLogger class.
 * 
 * @author Ben Jones
 */
public class ProcessLoggerTest {
	
	/**
	 * run method exhauses input stream with null logger
	 */
	@Test
	public void run_ExhaustsInputStreamWithNullLogger() {
		// Setup test mocking
		Process process = mock(Process.class);
		ByteArrayInputStream bais = new ByteArrayInputStream("a".getBytes());
		when(process.getInputStream()).thenReturn(bais);
		
		// Setup object under test and run test methods
		ProcessLogger processLogger = new ProcessLogger(process, null);
		processLogger.run();
		
		// Expect that input stream has been exhausted
		assertEquals("input stream exhausted", 0, bais.available());
	}
	
	/**
	 * run method exhausts input stream with logger
	 */
	@Test
	public void run_ExhaustsInputStreamWithLogger() {
		// Setup test mocking
		Logger logger = mock(Logger.class);
		Process process = mock(Process.class);
		ByteArrayInputStream bais = new ByteArrayInputStream("a".getBytes());
		when(process.getInputStream()).thenReturn(bais);
		
		// Setup object under test and run test methods
		ProcessLogger processLogger = new ProcessLogger(process, logger);
		processLogger.run();
		
		// Expect that input stream has been exhausted
		assertEquals("input stream exhausted", 0, bais.available());
	}
	
	/**
	 * run method doesn't wait for the process to exit with a null logger
	 */
	@Test
	public void run_DoesntWaitForProcessExitWithNullLogger() {
		// Setup test mocking
		Process process = mock(Process.class);
		ByteArrayInputStream bais = new ByteArrayInputStream("a".getBytes());
		when(process.getInputStream()).thenReturn(bais);
		
		// Setup object under test and run test methods
		ProcessLogger processLogger = new ProcessLogger(process, null);
		processLogger.run();

		// Expect PL doesn't wait for process to exit
		try { verify(process, times(0)).waitFor(); } catch (InterruptedException e) {}
	}
	
	/**
	 * run method waits for the process to exit with logger
	 */
	@Test
	public void run_WaitsForProcessExitWithLogger() {
		// Setup test mocking
		Logger logger = mock(Logger.class);
		Process process = mock(Process.class);
		ByteArrayInputStream bais = new ByteArrayInputStream("a".getBytes());
		when(process.getInputStream()).thenReturn(bais);
		
		// Setup object under test and run test methods
		ProcessLogger processLogger = new ProcessLogger(process, logger);
		processLogger.run();
		
		// Expect that PL waits for process to exit
		try { verify(process, times(1)).waitFor(); } catch (InterruptedException e) {}
	}
	
	/**
	 * run method logs process output and exit code with logger
	 */
	@Test
	public void run_LogsWithLogger() {
		// Setup test mocking
		Logger logger = mock(Logger.class);
		Process process = mock(Process.class);
		ByteArrayInputStream bais = new ByteArrayInputStream("a".getBytes());
		when(process.getInputStream()).thenReturn(bais);
		
		// Setup object under test and run test methods
		ProcessLogger processLogger = new ProcessLogger(process, logger);
		processLogger.run();
		
		// Expect two calls to log; the 'a' and the exit value
		verify(logger, times(2)).log(any(Level.class), anyString());
	}
	
}
