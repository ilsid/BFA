package com.ilsid.bfa.common;

/**
 * Number manipulation routines.
 * 
 * @author illia.sydorovych
 *
 */
public class NumberUtil {

	/**
	 * Checks whether the passed string represents double value.
	 * 
	 * @param str
	 *            the string to check
	 * @return <code>true</code> if the string is double or <code>false</code>
	 *         otherwise
	 */
	public static boolean isDouble(String str) {
		if (str == null) {
			return false;
		}

		int length = str.length();
		if (length == 0) {
			return false;
		}

		if (str.charAt(length - 1) == '.') {
			return false;
		}

		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}

		boolean dotMet = false;
		if (str.charAt(0) == '.') {
			if (length == 1) {
				return false;
			}
			i = 1;
			dotMet = true;
		}

		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				if (c == '.') {
					if (dotMet) {
						return false;
					} else {
						dotMet = true;
						continue;
					}
				}
				return false;
			}
		}

		if (!dotMet) {
			return false;
		}

		return true;
	}
	
	/**
	 * Checks whether the passed string represents integer value.
	 * 
	 * @param str
	 *            the string to check
	 * @return <code>true</code> if the string is integer or <code>false</code>
	 *         otherwise
	 */
	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}

		int length = str.length();
		if (length == 0) {
			return false;
		}

		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}

		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				return false;
			}
		}

		return true;
	}
	
	/**
	 * Checks whether the passed string represents positive integer value.
	 * 
	 * @param str
	 *            the string to check
	 * @return <code>true</code> if the string is positive integer or <code>false</code>
	 *         otherwise
	 */
	public static boolean isPositiveInteger(String str) {
		if (str == null) {
			return false;
		}

		int length = str.length();
		if (length == 0) {
			return false;
		}

		if (str.charAt(0) == '-') {
			return false;
		}
		
		if (str.charAt(0) == '0') {
			return false;
		}

		for (int i = 0; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				return false;
			}
		}

		return true;
	}
}
