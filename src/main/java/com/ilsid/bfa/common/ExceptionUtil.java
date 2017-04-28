package com.ilsid.bfa.common;

import java.util.List;

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
	public static String getExceptionMessageChain(Throwable exception) {
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

	/**
	 * Transforms error messages into a single {@link Exception} instance.
	 * 
	 * @param messages
	 *            error messages
	 * @return {@link Exception} instance with no root cause and with a composite message containing all passing
	 *         messages
	 */
	public static Exception toException(List<String> messages) {
		StringBuilder sb = new StringBuilder();
		for (String msg : messages) {
			sb.append(msg).append(StringUtils.LF);
		}

		return new Exception(sb.toString());
	}

}