package org.codehaus.jstestrunner;

/**
 * Represents something that can produce test results.
 */
public interface TestResultProducer {

	/**
	 * Determines whether the test result producer is available or not.
	 * 
	 * @return true if available
	 */
	boolean isAvailable();

}
