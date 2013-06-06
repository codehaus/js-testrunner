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

package org.codehaus.jstestrunner.junit;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jstestrunner.JSTestExecutionServer;
import org.codehaus.jstestrunner.JSTestSuiteRunnerService;
import org.codehaus.jstestrunner.jetty.JSTestResultHandler;
import org.codehaus.jstestrunner.jetty.JSTestResultHandler.JSTestResult;
import org.codehaus.jstestrunner.jetty.JSTestResultServer;
import org.eclipse.jetty.server.Server;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * A JavaScript Test Runner Suite specifically for JUnit. It allows a pattern of
 * URLs to be requested that will cause tests to occur. The test runner will
 * first ensure that a test results server container is started. By default the
 * test results server will listen for requests on 0.0.0.0:9080. Theses defaults
 * can be overridden.
 * 
 * <p>
 * Once the test results server started it will then map the context path for
 * serving tests to the target/classes and target/test-classes folders. The
 * default includes policy is to load all files matching the pattern
 * "**\/*Test.html" and "**\/*Test.htm". There is no default excludes.
 * 
 * <p>
 * A test execution server is also managed as a separate process. The command
 * used by the test runner is expected as a system property named
 * "org.codehaus.jstestrunner.commandPattern".
 * 
 * <p>
 * 
 * @see JSTestSuiteRunnerService for more information.
 * 
 * @author Christopher Hunt
 */
public class JSTestSuiteRunner extends ParentRunner<URL> {

	/**
	 * Describes the context path used by the test results server. Defaults to
	 * "/".
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface ContextPath {
		String value();
	}

	/**
	 * Describes the URL patterns that are to be excluded for test execution.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Exclude {
		String[] value();
	}

	/**
	 * Describes the host and port of the test results server using the
	 * host:port convention.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Host {
		String value();
	}

	/**
	 * Describes the URL patterns that are to be included for test execution.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface Include {
		String[] value();
	}

	/**
	 * A JavaScript execution failure object.
	 */
	private static class JSTestFailure extends Failure {
		private final URL url;

		public JSTestFailure(final Description description, final URL url, final String message) {
			super(description, new RuntimeException(message));
			this.url = url;
		}

		@Override
		public String getTestHeader() {
			return JSTestSuiteRunnerService.getFormattedPath(url);
		}

		@Override
		public String getTrace() {
			// The stack means nothing here.
			return getMessage();
		}

		@Override
		public String toString() {
			return getTestHeader();
		}

	}

	/**
	 * Describes where in relation to the project home folder the context path
	 * should be mapped to. This can have multiple values. The defaults are
	 * "target/classes" and "target/test-classes" given Maven's convention.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface ResourceBase {
		String[] value();
	}

	/**
	 * Describes where in relation to the project home folder the test runner
	 * file can be made available from for execution purposes. This file runs on
	 * the JS test execution engine and processes each file for testing. By
	 * default the target/js-testrunner folder is used.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	public @interface TestRunnerFilePath {
		String value();
	}

	private final JSTestSuiteRunnerService jSTestSuiteRunnerService;
	private final List<URL> urls;

	public JSTestSuiteRunner(final Class<?> testClass) throws InitializationError {
		super(testClass);

		// We don't care to see what these packages are up to unless there's
		// some complaining to be done.
		final Logger logger = Logger.getLogger("org.eclipse.jetty");
		logger.setLevel(Level.WARNING);

		// Set up our host.
		final Host hostAnnotation = testClass.getAnnotation(Host.class);
		String host;
		int port;
		if (hostAnnotation == null) {
			host = "localhost";
			port = 9080;

		} else {
			final String[] hostParts = hostAnnotation.value().split(":");
			if (hostParts.length != 2) {
				throw new InitializationError(
						"Host must be of the form host:port");
			}
			host = hostParts[0];
			port = Integer.valueOf(hostParts[1]);

		}

		// Set up our context path.
		final ContextPath contextPathAnnotation = testClass
				.getAnnotation(ContextPath.class);
		String contextPath;
		if (contextPathAnnotation == null) {
			contextPath = "/";
		} else {
			contextPath = contextPathAnnotation.value();
		}

		// Set up our resource bases.
		final ResourceBase resourceBaseAnnotation = testClass
				.getAnnotation(ResourceBase.class);
		String[] resourceBases;
		if (resourceBaseAnnotation == null) {
			resourceBases = new String[] {
					"target" + File.separator + "classes",
					"target" + File.separator + "test-classes" };
		} else {
			resourceBases = resourceBaseAnnotation.value();
		}

		// Inclusion patterns
		final Include includeAnnotation = testClass.getAnnotation(Include.class);
		String[] includes;
		if (includeAnnotation == null) {
			includes = new String[] { "**/*Test.html", "**/*Test.htm" };
		} else {
			includes = includeAnnotation.value();
		}

		// Inclusion patterns
		final Exclude excludeAnnotation = testClass.getAnnotation(Exclude.class);
		String[] excludes;
		if (excludeAnnotation == null) {
			excludes = new String[0];
		} else {
			excludes = excludeAnnotation.value();
		}

		// Resolve the commandPattern from system properties.
		String commandPattern = System
				.getProperty("org.codehaus.jstestrunner.commandPattern");
		if (commandPattern == null) {
			commandPattern = "phantomjs '%1$s' %2$s";
		}

		// Test runner file path.
		final TestRunnerFilePath testRunnerFilePathAnnotation = testClass
				.getAnnotation(TestRunnerFilePath.class);
		String testRunnerFilePath;
		if (testRunnerFilePathAnnotation == null) {
			testRunnerFilePath = "target" + File.separator + "js-testrunner";
		} else {
			testRunnerFilePath = testRunnerFilePathAnnotation.value();
		}

		/**
		 * Determine the URLs representing the tests.
		 */
		urls = JSTestSuiteRunnerService.scanTestFiles(host, port,
				resourceBases, includes, excludes);

        final JSTestResultServer jSTestResultServer = new JSTestResultServer(new Server(), new JSTestResultHandler(),
                port, contextPath, resourceBases);

		final JSTestExecutionServer jSTestExecutionServer = new JSTestExecutionServer(testRunnerFilePath,
                commandPattern, urls);

        jSTestSuiteRunnerService = new JSTestSuiteRunnerService(jSTestResultServer, jSTestExecutionServer);

	}

	/**
	 * Clean up our test environment.
	 * 
	 * @param statement
	 *            the statement we should append to.
	 * @return the appended statement.
	 */
	private Statement afterTests(final Statement statement) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					// Evaluate all that comes before this point.
					statement.evaluate();
				} finally {
					jSTestSuiteRunnerService.afterTests();
				}
			}
		};
	}

	/**
	 * Establish our test environment.
	 * 
	 * @param statement
	 *            the statement to prepend.
	 * @return the prepended statement.
	 */
	private Statement beforeTests(final Statement statement) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				jSTestSuiteRunnerService.beforeTests();

				// Evaluate the remaining statements.
				statement.evaluate();
			}
		};
	}

	@Override
	protected Statement classBlock(final RunNotifier notifier) {
		final Statement statement1 = super.classBlock(notifier);
        final Statement statement2 = beforeTests(statement1);
        final Statement statement3 = afterTests(statement2);
		return statement3;
	}

	@Override
	protected Description describeChild(final URL url) {
		return Description
				.createTestDescription(this.getTestClass().getJavaClass(),
						JSTestSuiteRunnerService.getFormattedPath(url));
	}

	@Override
	protected List<URL> getChildren() {
		return urls;
	}

	public JSTestSuiteRunnerService getjSTestSuiteRunnerService() {
		return jSTestSuiteRunnerService;
	}

	@Override
	protected void runChild(final URL url, final RunNotifier notifier) {
		final Description description = describeChild(url);
		notifier.fireTestStarted(description);
		try {
			final JSTestResult jsTestResult = jSTestSuiteRunnerService.runTest(url);
			if (jsTestResult != null) {
				if (jsTestResult.getFailures() > 0) {
					final JSTestFailure failure = new JSTestFailure(description, url,
							"Failures: " + jsTestResult.getFailures() + ", passes: "
									+ jsTestResult.getPasses() + ":\n"
									+ jsTestResult.getMessage());
					notifier.fireTestFailure(failure);
				}
			} else {
				final JSTestFailure failure = new JSTestFailure(description, url,
						"Timed out waiting for test");
				notifier.fireTestFailure(failure);
			}
		} finally {
			notifier.fireTestFinished(description);

		}

	}

}
