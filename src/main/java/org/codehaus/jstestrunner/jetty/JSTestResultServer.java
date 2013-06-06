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

package org.codehaus.jstestrunner.jetty;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codehaus.jstestrunner.TestResultProducer;
import org.codehaus.jstestrunner.jetty.JSTestResultHandler.JSTestResult;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

/**
 * A server responsible for obtaining results from an execution.
 */
public class JSTestResultServer {

	private final Server webServer;

    private final JSTestResultHandler jsTestResultHandler;

	private final Integer port;

	private final String contextPath;

    private final String[] resourceBases;

    /**
     * @param webServer The Jetty server instance
     * @param jsTestResultHandler The handler of test results (POST requests)
     */
    public JSTestResultServer(final Server webServer, final JSTestResultHandler jsTestResultHandler,
                              final Integer port, final String contextPath, final String[] resourceBases) {
        this.webServer = webServer;
        this.jsTestResultHandler = jsTestResultHandler;
        this.port = port;
        this.contextPath = contextPath;
        this.resourceBases = resourceBases;
    }

    /**
	 * Set when the web server has been initialised.
	 */
	private final AtomicBoolean initedWebServer = new AtomicBoolean(false);

	/**
	 * Get a result for a given URL.
	 * 
	 * @param url
	 *            the url to obtain the result for.
	 * @param testResultProducer
	 *            Used to determine whether we are in a position to wait for
	 *            results.
	 * @return the test result or null if one cannot be obtained.
	 */
	public JSTestResult getJsTestResult(final URL url,
			final TestResultProducer testResultProducer) {
		return jsTestResultHandler.getJsTestResult(url, testResultProducer, 30,
				TimeUnit.SECONDS);
	}

	/**
	 * Initialise the web server.
	 */
	private void initWebServer() {

		final ServerConnector connector = new ServerConnector(webServer);
		connector.setPort(port);
		webServer.addConnector(connector);

		final Handler[] handlers = new Handler[resourceBases.length + 1];
		int i = 0;
		for (String resourceBase : resourceBases) {
			final ResourceHandler resourceHandler = new ResourceHandler();
			resourceHandler.setResourceBase(resourceBase);
			handlers[i++] = resourceHandler;
		}
		handlers[i] = jsTestResultHandler;

		HandlerList handlerList = new HandlerList();
		handlerList.setHandlers(handlers);

		ContextHandler contextHandler = new ContextHandler();
		contextHandler.setContextPath(contextPath);
		contextHandler.setHandler(handlerList);

		webServer.setHandler(contextHandler);

	}

	/**
	 * Start the server.
	 * 
	 * @throws Exception
	 *             if something goes wrong.
	 */
	public void start() throws Exception {
		if (!initedWebServer.getAndSet(true)) {
			initWebServer();
		}
		webServer.start();
	}

	/**
	 * Stop the server.
	 * 
	 * @throws Exception
	 *             if something goes wrong.
	 */
	public void stop() throws Exception {
		if (initedWebServer.get()) {
			webServer.stop();
		}
	}

    public Server getWebServer() {
        return webServer;
    }

    public JSTestResultHandler getJsTestResultHandler() {
        return jsTestResultHandler;
    }

    public Integer getPort() {
        return port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String[] getResourceBases() {
        return resourceBases;
    }

}
