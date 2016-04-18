package com.ilsid.bfa.persistence.orientdb;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.NumberUtil;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * The base class for OrientDB DAO.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class OrientdbRepository implements Configurable {

	private static final String CONFIG_PROP_DB_URL = "bfa.persistence.orientdb.url";

	private static final String CONFIG_PROP_DB_USER = "bfa.persistence.orientdb.user";

	private static final String CONFIG_PROP_DB_PWD = "bfa.persistence.orientdb.password";

	private static final String CONFIG_PROP_DB_POOL_SIZE = "bfa.persistence.orientdb.pool_size";

	private static final int DB_POOL_SIZE_DEFAULT_VALUE = 10;

	private static OrientGraphFactory factory;

	private static final Object FACTORY_LOCK = new Object();

	/**
	 * Executes a database callback. A caller must handle transactions (begin/commit/rollback).
	 * 
	 * @param callback
	 *            database callback
	 * @throws PersistenceException
	 *             in case of callback failure
	 * @return callback's result
	 */
	protected <T> T execute(DatabaseCallback<T> callback) throws PersistenceException {
		final T result;
		OrientGraph connection = getConnection();

		try {
			result = callback.doInDatabase(connection);
		} catch (RuntimeException e) {
			throw new PersistenceException("Database operation failed", e);
		} finally {
			connection.shutdown();
		}

		return result;
	}

	/**
	 * Executes a database callback in a single transaction. Transaction is rolled back in case of callback failure
	 * (when it throws {@link PersistenceException}).
	 * 
	 * @param callback
	 *            database callback
	 * @throws PersistenceException
	 *             in case of callback failure
	 * @return callback's result
	 */
	protected <T> T executeInTransaction(DatabaseCallback<T> callback) throws PersistenceException {
		final T result;
		OrientGraph connection = getConnection();
		ODatabaseDocumentTx rawConnection = connection.getRawGraph();

		rawConnection.begin();
		try {
			try {
				result = callback.doInDatabase(connection);
			} catch (RuntimeException e) {
				rawConnection.rollback();
				throw new PersistenceException("Database operation failed", e);
			} catch (PersistenceException e) {
				rawConnection.rollback();
				throw e;
			}
			rawConnection.commit();
		} finally {
			connection.shutdown();
		}

		return result;
	}

	private OrientGraph getConnection() throws OrientdbSystemException {
		OrientGraph graph;
		// FIXME:
		// Re-try logic should be here. getTx() throws RuntimeException when no connections are available in the pool.
		try {
			graph = factory.getTx();
		} catch (RuntimeException e) {
			throw new OrientdbSystemException("Failed to get a database connection", e);
		}

		return graph;
	}

	/**
	 * Initializes database connection pool using the provided configuration. Should be invoked only once. All
	 * {@link OrientdbRepository} instances share the same pool. Does nothing on consequent invocations.
	 * 
	 * @param config
	 *            database configuration
	 * @throws ConfigurationException
	 *             in case of incorrect configuration or pool initialization failure
	 */
	@Inject
	@Override
	public void setConfiguration(@RepositoryConfig Map<String, String> config) throws ConfigurationException {
		synchronized (FACTORY_LOCK) {
			if (factory != null) {
				return;
			}

			List<String> missedProps = new LinkedList<>();
			String url = getRequiredValue(CONFIG_PROP_DB_URL, config, missedProps);
			String user = getRequiredValue(CONFIG_PROP_DB_USER, config, missedProps);
			String pwd = getRequiredValue(CONFIG_PROP_DB_PWD, config, missedProps);

			if (missedProps.size() > 0) {
				throw new ConfigurationException(String.format("The required configuration properties are missed: %s",
						StringUtils.join(missedProps, ", ")));
			}

			int poolSize = getOptionalPositiveIntegerValue(CONFIG_PROP_DB_POOL_SIZE, config,
					DB_POOL_SIZE_DEFAULT_VALUE);

			try {
				factory = new OrientGraphFactory(url, user, pwd).setupPool(1, poolSize);
				factory.setAutoStartTx(false);
			} catch (RuntimeException e) {
				throw new ConfigurationException("Failed to create a database connection factory", e);
			}
		}
	}

	static void closeFactory() {
		synchronized (FACTORY_LOCK) {
			if (factory != null) {
				factory.close();
				factory = null;
			}
		}
	}

	private int getOptionalPositiveIntegerValue(String propName, Map<String, String> config, int defaultValue)
			throws ConfigurationException {
		String strValue = config.get(propName);

		int intValue;
		if (NumberUtil.isPositiveInteger(strValue)) {
			intValue = Integer.parseInt(strValue);
		} else if (strValue == null) {
			intValue = defaultValue;
		} else {
			throw new ConfigurationException(
					String.format("The value of the configuration property [%s] must be positive integer", propName));
		}

		return intValue;
	}

	private String getRequiredValue(String propName, Map<String, String> config, List<String> missedProps) {
		String res = config.get(propName);

		if (res == null) {
			missedProps.add(propName);
		}

		return res;
	}

}
