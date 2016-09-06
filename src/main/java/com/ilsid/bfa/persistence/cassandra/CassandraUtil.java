package com.ilsid.bfa.persistence.cassandra;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.ilsid.bfa.common.ExceptionUtil;

/**
 * Provides Cassandra specific routines.
 * 
 * @author illia.sydorovych
 *
 */
public class CassandraUtil {

	private static final SimpleDateFormat TOKEN_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	/**
	 * Extracts date token in <i>yyyyMMdd</i> format from the given time stamp.
	 * 
	 * @param timestamp
	 *            time stamp
	 * @return date token
	 */
	public static String timestampToDateToken(Date timestamp) {
		return TOKEN_DATE_FORMAT.format(timestamp);
	}

	/**
	 * Extracts error messages chain.
	 * 
	 * @param error
	 *            error to process
	 * @return error messages chain or <code>null</code> in case of <code>null</code> argument
	 */
	public static List<String> getErrorDetails(Exception error) {
		if (error == null) {
			return null;
		}

		String[] chain = ExceptionUtil.getExceptionMessageChain(error).split(StringUtils.LF);
		return Arrays.asList(chain);
	}

}
