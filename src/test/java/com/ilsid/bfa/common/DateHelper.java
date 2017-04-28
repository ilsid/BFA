package com.ilsid.bfa.common;

import java.util.Date;

public class DateHelper {

	private static final long ONE_MINUTE_IN_MILLIS = 60000;

	private static final long ONE_DAY_IN_MILLIS = 86400000;

	public static Date addMinutes(Date time, int minutes) {
		return new Date(time.getTime() + minutes * ONE_MINUTE_IN_MILLIS);
	}

	public static Date addDays(Date time, int days) {
		return new Date(time.getTime() + days * ONE_DAY_IN_MILLIS);
	}

}
