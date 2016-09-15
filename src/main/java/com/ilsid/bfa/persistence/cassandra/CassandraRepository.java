package com.ilsid.bfa.persistence.cassandra;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.persistence.RepositoryConfig;

/**
 * Cassandra DBMS based repository.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class CassandraRepository implements Configurable {

	private static final Object CLUSTER_LOCK = new Object();

	private static Cluster cluster;

	private static Session session;

	/**
	 * Returns Cassandra session. Must be used only after {@link #setConfiguration(Map)} invocation.
	 * 
	 * @return a database session or <code>null</code> if {@link #setConfiguration(Map)} was never invoked
	 * @see #setConfiguration(Map)
	 */
	protected Session getSession() {
		return session;
	}

	protected boolean useDefaultKeyspace() {
		return true;
	}

	/**
	 * Initializes database session using the provided configuration. Should be invoked only once. All
	 * {@link CassandraRepository} instances share the same session. Does nothing on consequent invocations.
	 * 
	 * @param config
	 *            database configuration
	 * @throws ConfigurationException
	 *             in case of incorrect configuration or pool initialization failure
	 */
	@Inject
	@Override
	public void setConfiguration(@RepositoryConfig Map<String, String> config) throws ConfigurationException {
		synchronized (CLUSTER_LOCK) {
			if (cluster != null) {
				return;
			}

			Map<String, Integer> contactPoints = CassandraConfig.extractContactPoints(config);

			List<InetSocketAddress> addresses = new LinkedList<>();
			for (String host : contactPoints.keySet()) {
				addresses.add(new InetSocketAddress(host, contactPoints.get(host)));
			}

			PoolingOptions poolingOptions = CassandraConfig.extractPoolingOptions(config);
			SocketOptions socketOptions = new SocketOptions().setConnectTimeoutMillis(2000);

			cluster = Cluster.builder().addContactPointsWithPorts(addresses).withPoolingOptions(poolingOptions)
					.withSocketOptions(socketOptions).build();

			if (useDefaultKeyspace()) {
				session = cluster.connect(CassandraConfig.KEYSPACE_NAME);
			} else {
				session = cluster.connect();
			}
		}

	}

	static void closeConnection() {
		synchronized (CLUSTER_LOCK) {
			if (cluster != null) {
				try {
					if (session != null) {
						session.close();
					}
				} finally {
					cluster.close();
					cluster = null;
				}
			}
		}
	}

}
