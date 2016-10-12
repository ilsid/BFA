package com.ilsid.bfa.persistence.cassandra;

import java.text.SimpleDateFormat;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class CassandraUtilUnitTest extends BaseUnitTestCase {

	@Test
	public void dateTokenCanBeExtracted() throws Exception {
		assertEquals("20160912", CassandraUtil
				.timestampToDateToken(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").parse("2016/09/12 12:50:45")));
	}

}
