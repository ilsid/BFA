package com.ilsid.bfa.persistence.cassandra;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PoolingOptions;
import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;

/**
 * Cassandra DBMS based repository.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class CassandraRepository implements Configurable {

	private static final Object CLUSTER_LOCK = new Object();

	private static Cluster cluster;

	/**
	 * Initializes database connection pool using the provided configuration. Should be invoked only once. All
	 * {@link CassandraRepository} instances share the same pool. Does nothing on consequent invocations.
	 * 
	 * @param config
	 *            database configuration
	 * @throws ConfigurationException
	 *             in case of incorrect configuration or pool initialization failure
	 */
	@Override
	public void setConfiguration(Map<String, String> config) throws ConfigurationException {
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

			cluster = Cluster.builder().addContactPointsWithPorts(addresses).withPoolingOptions(poolingOptions).build();
		}

	}

	void closeCluster() {
		synchronized (CLUSTER_LOCK) {
			if (cluster != null) {
				cluster.close();
				cluster = null;
			}
		}
	}

}
