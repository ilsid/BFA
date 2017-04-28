package com.ilsid.bfa.persistence.cassandra;

import java.text.SimpleDateFormat;
import java.util.Date;

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

}
