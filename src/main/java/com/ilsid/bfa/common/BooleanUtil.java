package com.ilsid.bfa.common;

/**
 * Boolean manipulation routines.
 * 
 * @author illia.sydorovych
 *
 */
public class BooleanUtil {

	private static final String VALID_TRUE_VALUE = "true";

	private static final String VALID_FALSE_VALUE = "false";

	/**
	 * Determines whether the given string represents boolean.
	 * 
	 * @param str
	 *            string to examine
	 * @return <code>true</code> if the passed string is "true" or "false" ignoring case, and <code>false</code>
	 *         otherwise
	 */
	public static boolean isBoolean(String str) {
		return VALID_TRUE_VALUE.equalsIgnoreCase(str) || VALID_FALSE_VALUE.equalsIgnoreCase(str);
	}

}
