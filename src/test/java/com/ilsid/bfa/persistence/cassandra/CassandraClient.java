package com.ilsid.bfa.persistence.cassandra;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;

public class CassandraClient extends CassandraRepository {

	private static final String KEYSPACE_CREATION_SCRIPT = "src/main/resources/database/cassandra-keyspace.cql";

	private static final Pattern ALPHA_PATTERN = Pattern.compile(".*[a-zA-Z].*", Pattern.DOTALL);

	@Override
	protected boolean useDefaultKeyspace() {
		return false;
	}

	public ResultSet queryWithAllowedFiltering(String query) {
		return getSession().execute(query + " ALLOW FILTERING");
	}

	void initDatabase() throws Exception {
		String script = FileUtils.readFileToString(new File(KEYSPACE_CREATION_SCRIPT));
		String[] statements = script.split(";");
		final Session session = getSession();

		for (String stmt : statements) {
			if (ALPHA_PATTERN.matcher(stmt).matches()) {
				session.execute(new SimpleStatement(stmt));
			}
		}
	}

	void dropDatabase() {
		getSession().execute("DROP KEYSPACE " + CassandraConfig.KEYSPACE_NAME);
	}

}
