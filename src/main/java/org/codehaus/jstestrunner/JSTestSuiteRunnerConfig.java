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

import java.net.URL;
import java.util.List;

import org.codehaus.jstestrunner.jetty.JSTestResultServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the runner.
 */
@Configuration
public class JSTestSuiteRunnerConfig {

	private String commandPattern;

	private String contextPath;

	private String[] excludes;

	private String[] includes;

	private String host;

	private int port;

	private String[] resourceBases;

	private String testRunnerFilePath;

	private List<URL> urls;

	@Bean
	public String commandPattern() {
		return commandPattern;
	}

	@Bean
	public String contextPath() {
		return contextPath;
	}

	@Bean
	public String[] excludes() {
		return excludes;
	}

	@Bean
	public String host() {
		return host;
	}

	@Bean
	public String[] includes() {
		return includes;
	}

	/**
	 * Allows external source to initialise the configuration.
	 */
	public void init(String commandPattern, String contextPath,
			String[] excludes, String host, String[] includes, int port,
			String[] resourceBases, String testRunnerFilePath, List<URL> urls) {
		this.commandPattern = commandPattern;
		this.contextPath = contextPath;
		this.excludes = excludes;
		this.host = host;
		this.includes = includes;
		this.port = port;
		this.resourceBases = resourceBases;
		this.testRunnerFilePath = testRunnerFilePath;
		this.urls = urls;
	}

	@Bean
	public JSTestExecutionServer jSTestExecutionServer() {
		return new JSTestExecutionServer();
	}

	@Bean
	public JSTestResultServer jSTestResultServer() {
		return new JSTestResultServer();
	}

	@Bean
	public JSTestSuiteRunnerService jSTestSuiteRunnerService() {
		return new JSTestSuiteRunnerService();
	}

	@Bean
	public Integer port() {
		return port;
	}

	@Bean
	public String[] resourceBases() {
		return resourceBases;
	}

	public void setTestRunnerFilePath(String testRunnerFilePath) {
		this.testRunnerFilePath = testRunnerFilePath;
	}

	@Bean
	public String testRunnerFilePath() {
		return testRunnerFilePath;
	}

	@Bean
	public List<URL> urls() {
		return urls;
	}
}
