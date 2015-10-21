package com.ilsid.bfa.common;

import org.apache.commons.lang3.StringUtils;

/**
 * Exception handling routines.
 * 
 * @author illia.sydorovych
 * 
 */
public class ExceptionUtil {

	private static final String CAUSEDBY_STR = "   Caused by: ";

	/**
	 * Returns a message chain of the exception's stack trace.
	 * 
	 * @param exception
	 *            exception
	 * @return the exception message chain
	 */
	public static String getExceptionMessageChain(Exception exception) {
		StringBuilder chain = new StringBuilder();
		chain.append(exception.getMessage());

		Throwable cause = exception.getCause();
		while (cause != null) {
			String causeMsg = cause.getMessage();
			if (!StringUtils.isBlank(causeMsg)) {
				chain.append(StringUtils.LF).append(CAUSEDBY_STR).append(causeMsg);
			}
			cause = cause.getCause();
		}

		return chain.toString();
	}

}