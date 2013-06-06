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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.codehaus.jstestrunner.jetty.JSTestResultHandler.JSTestResult;
import org.codehaus.jstestrunner.jetty.JSTestResultServer;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * A JavaScript Test Runner Suite service that is not tied to any particular
 * testing framework.
 * 
 * @author Christopher Hunt
 * 
 */
public class JSTestSuiteRunnerService {

    private static final Logger LOGGER = Logger.getLogger(JSTestSuiteRunnerService.class
            .getName());

    /**
	 * Tidy up the path for output.
	 * 
	 * @param url
	 *            the url to tidy.
	 * @return the tidied string representation for output.
	 */
	public static String getFormattedPath(final URL url) {
		final String urlPath = url.getPath();
        String strippedUrlPath;
		if (urlPath.length() > 0 && urlPath.charAt(0) == '/') {
			// Looks better without the leading '/'.
            strippedUrlPath = urlPath.substring(1);
		} else {
            strippedUrlPath = urlPath;
        }
		return strippedUrlPath;
	}

	/**
	 * Convert an array of relative file paths to URLs.
	 * 
	 * @param includedFiles
	 *            the relative file paths.
	 * @return a collection of relative urls.
	 */
	private static Collection<URL> relativeFilepathsAsUrls(final String host,
			final int port, final String[] includedFiles) {
		final Collection<URL> relativeUrls = new ArrayList<URL>(includedFiles.length);
		for (final String includedFile : includedFiles) {
			try {
				relativeUrls.add(new URL("http", host, port, "/"
						+ includedFile.replace(File.separatorChar, '/')));
			} catch (MalformedURLException e) {
                LOGGER.severe("Problem determining relative urls: " + e.getMessage());
            }
		}
		return relativeUrls;
	}

	/**
	 * Return a list of files for each resource base that matches the patterns
	 * of includes and excludes.
	 * 
	 * @return the list of urls
	 */
	public static List<URL> scanTestFiles(final String host, final int port,
			final String[] resourceBases, final String[] includes, final String[] excludes) {
		final List<URL> includedUrls = new ArrayList<URL>();
		final DirectoryScanner scanner = new DirectoryScanner();
		scanner.setIncludes(includes);
		scanner.setExcludes(excludes);
		for (final String resourceBase : resourceBases) {
			scanner.setBasedir(resourceBase);
			scanner.scan();
			includedUrls.addAll(relativeFilepathsAsUrls(host, port,
					scanner.getIncludedFiles()));
		}
		return includedUrls;
	}

	private final JSTestResultServer jSTestResultServer;

	private final JSTestExecutionServer jSTestExecutionServer;

    public JSTestSuiteRunnerService(
            final JSTestResultServer jSTestResultServer, final JSTestExecutionServer jSTestExecutionServer) {
        this.jSTestResultServer = jSTestResultServer;
        this.jSTestExecutionServer = jSTestExecutionServer;
    }

	/**
	 * Clean up our test environment.
	 * 
	 * @throws Exception
	 *             if something goes wrong.
	 */
	public void afterTests() throws Exception {
		try {
			// Tear down the test execution server.
			jSTestExecutionServer.stop();
		} finally {
			// Tear down the test results server.
			jSTestResultServer.stop();
		}
	}

	/**
	 * Establish our test environment.
	 * 
	 * @throws Exception
	 *             if something goes wrong.
	 * 
	 */
	public void beforeTests() throws Exception {
		// Establish the test results server.
		jSTestResultServer.start();

		// Establish the test execution server.
		jSTestExecutionServer.start();
	}

	public JSTestExecutionServer getjSTestExecutionServer() {
		return jSTestExecutionServer;
	}

	public JSTestResultServer getjSTestResultServer() {
		return jSTestResultServer;
	}

	/**
	 * Run an actual test and return its result.
	 * 
	 * @param url
	 *            the url representing the test.
	 * @return the test result.
	 */
	public JSTestResult runTest(final URL url) {
		return jSTestResultServer.getJsTestResult(url, jSTestExecutionServer);
	}
}
