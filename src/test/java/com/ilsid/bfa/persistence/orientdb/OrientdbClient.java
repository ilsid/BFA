package com.ilsid.bfa.persistence.orientdb;

import java.util.List;
import java.util.Map;

import com.ilsid.bfa.TestConstants;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientdbClient {

	private static final Object FACTORY_LOCK = new Object();

	private static OrientGraphFactory factory;

	public static void init() {
		synchronized (FACTORY_LOCK) {
			if (factory == null) {
				factory = new OrientGraphFactory(TestConstants.DATABASE_URL, TestConstants.DATABASE_USER,
						TestConstants.DATABASE_PASSWORD).setupPool(1, 10);
				factory.setAutoStartTx(false);
			}
		}
	}

	public static void release() {
		synchronized (FACTORY_LOCK) {
			if (factory != null) {
				factory.close();
			}
		}
	}

	public static List<ODocument> query(String statement) {
		return query(statement, null);
	}

	public static List<ODocument> query(String statement, Map<String, Object> params) {
		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>(statement);
		List<ODocument> result;
		
		final OrientGraph connection = factory.getTx();
		try {
			if (params != null) {
				result = connection.getRawGraph().command(query).execute(params);
			} else {
				result = connection.getRawGraph().command(query).execute();
			}
		} finally {
			connection.shutdown();
		}

		return result;
	}
}
