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

/*global console, phantom, WebPage */

/**
 * Process the test.
 */
function processTestAndLoadNext(testUrls) {
	var testPage, testUrl;
	
	testUrl = testUrls[0];
	testUrls.splice(0, 1);

	testPage = new WebPage();
	
	testPage.onConsoleMessage = function(msg) {
		console.log(msg);
	};

	testPage.open(testUrl, function(status) {
		var testResultChecker;
		
		if (status === "success"){
			testResultChecker = setInterval(function() {
				var testResultsProcessed;
				
				testResultsProcessed = testPage.evaluate(function() {
					var testResultsProcessed, testResults, testResultElem;
			
					/**
					 * Notify the notifier using an HTTP POST. We send it asynchronously and don't
					 * bother about a response. If tests fail at the consuming end then this should
					 * an exception rather than the rule. Failing tests are better than false
					 * positives.
					 * 
					 * @param testUrl
					 *            the url of the test document the test relates to.
					 * @param moduleName
					 *            the module. Can be null.
					 * @param testName
					 *            the name of the test.
					 * @param failed
					 *            the number of assertions failing.
					 * @param passed
					 *            the number of assertions passing.
					 */
					function notify(testUrl, testResults) {

						var failures, i, j, message, messages, passes, testResult, xhr;

						failures = 0;
						passes = 0;
						messages = "";

						for (j in testResults) {
							testResult = testResults[j];
							if (testResult.moduleName !== null) {
								message = "[" + testResult.moduleName + "] ";
							} else {
								message = "";
							}
							message += testResult.testName + ": failed: " + testResult.failed
									+ " passed: " + testResult.passed;
							for (i in testResult.details) {
								message += "\n  " + testResult.details[i].message + ", expected: "
										+ testResult.details[i].expected;
							}

							if (j !== "0") {
								messages += "\n";
							}
							messages += message;

							passes += testResult.passed;
							failures += testResult.failed;
						}

						xhr = new XMLHttpRequest();
						xhr.open("POST", "/testResults", false);
						xhr.setRequestHeader("Content-Type", "application/json");
						try {
							xhr.send(JSON.stringify({
								testUrl : testUrl,
								passes : passes,
								failures : failures,
								message : messages
							}));
						} catch (e) {
							// Just swallow exceptions as we can't do anything useful if there are
							// comms errors.
						}
					}

					/**
					 * Parse the document for test results.
					 */
					function getTestResults() {
						var details, failed, i, j, nodeList, nodeList2, moduleName, passed, testsElem, testItemElem, testItemElems, testName, testResults;

						testResults = [];

						testsElem = document.getElementById("qunit-tests");
						if (testsElem !== null) {

							testItemElems = testsElem.getElementsByTagName("li");

							// For each test, collect the test results
							for (i = 0; i < testItemElems.length; ++i) {
								// Extract the microformatted data.
								testItemElem = testItemElems[i];

								// Not interested in the detailed messages.
								if (testItemElem.parentNode === testsElem) {
									nodeList = testItemElem.getElementsByClassName("module-name");
									if (nodeList.length === 1) {
										moduleName = nodeList[0].innerText;
									} else {
										moduleName = null;
									}
									nodeList = testItemElem.getElementsByClassName("test-name");
									if (nodeList.length === 1) {
										testName = nodeList[0].innerText;
									} else {
										testName = null;
									}
									nodeList = testItemElem.getElementsByClassName("failed");
									if (nodeList.length === 1) {
										failed = parseInt(nodeList[0].innerText, 10);
									} else {
										failed = null;
									}
									nodeList = testItemElem.getElementsByClassName("passed");
									if (nodeList.length === 1) {
										passed = parseInt(nodeList[0].innerText, 10);
									} else {
										passed = null;
									}
									nodeList = testItemElem.getElementsByClassName("test-message");
									nodeList2 = testItemElem
											.getElementsByClassName("test-expected");
									details = [];
									for (j = 0; j < nodeList.length; ++j) {
										details.push({
											message : nodeList[j].innerText,
											expected : nodeList2[j].innerText
										});
									}

									testResults.push({
										moduleName : moduleName,
										testName : testName,
										failed : failed,
										passed : passed,
										details : details
									});
								}
							}

						} else {
							console.log("Cannot find #qunit-tests element. Skipping test results.");
							
						}
						
						return testResults;
					}

					testResultElem = document.getElementById("qunit-testresult");

					if (testResultElem && testResultElem.innerText.match("completed")) {

						// Tests are complete. Drill down and extract all the test
						// results from this one file.
			
						testResults = getTestResults();
						
						// Pass the data on for all tests per given test url.

						notify(document.location.href, testResults);

						// Signal that we're done processing the test results.
						
						testResultsProcessed = true;
						
					} else {
						testResultsProcessed = false;
					}
					
					return testResultsProcessed;
				});
			
				if (testResultsProcessed) {
					
					// Discontinue our checking for test results as we now have them.
					
					clearInterval(testResultChecker);
					
					// Run the next test or exit if no more.
				
					if (testUrls.length === 0) {
						phantom.exit();
					}
					
					processTestAndLoadNext(testUrls);
				}
				
			}, 100);

		} else {
			phantom.exit();
		}
	
	});
}

/**
 * Main control flow for Phantom.
 */
function main() {

	var testUrls;

	if (phantom.args.length === 1) {

		testUrls = phantom.args[0].split(',');

		if (testUrls.length > 0) {
			processTestAndLoadNext(testUrls);
		}

	} else {
		console.log("Usage: run-qunit.js URL[,URL]*");
		phantom.exit();

	}

}

main();