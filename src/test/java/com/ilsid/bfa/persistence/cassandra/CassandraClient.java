package com.ilsid.bfa.persistence.cassandra;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.ilsid.bfa.persistence.PersistenceException;

public class CassandraClient extends CassandraRepository {

	private static final String KEYSPACE_CREATION_SCRIPT = "src/main/resources/database/cassandra-keyspace.cql";

	private static final Pattern ALPHA_PATTERN = Pattern.compile(".*[a-zA-Z].*", Pattern.DOTALL);

	private final Map<String, PreparedStatement> preparedStatements = new HashMap<>();

	@Override
	protected void prepareStatements(Session session) throws PersistenceException {

	}

	@Override
	protected boolean useDefaultKeyspace() {
		return false;
	}

	public ResultSet query(String query) {
		return getSession().execute(query);
	}

	public ResultSet queryWithAllowedFiltering(String query) {
		return getSession().execute(query + " ALLOW FILTERING");
	}

	public void executeBoundStatement(String statement, Object... values) {
		Session session = getSession();

		PreparedStatement prepStmt = preparedStatements.get(statement);
		if (prepStmt == null) {
			prepStmt = session.prepare(statement);
			preparedStatements.put(statement, prepStmt);
		}

		session.execute(prepStmt.bind(values));
	}
	
	public void clearDatabase() {
		query("TRUNCATE failed_flows");
		query("TRUNCATE running_flows");
		query("TRUNCATE completed_flows");
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
