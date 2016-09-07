package com.ilsid.bfa.persistence.cassandra;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.ConfigUtil;

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

			List<InetSocketAddress> contactPoints = extractContactPoints(config);
			PoolingOptions poolingOptions = extractPoolingOptions(config);

			cluster = Cluster.builder().addContactPointsWithPorts(contactPoints).withPoolingOptions(poolingOptions)
					.build();
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

	private List<InetSocketAddress> extractContactPoints(Map<String, String> config) throws ConfigurationException {
		Map<String, Integer> contactPoints = CassandraConfig.extractContactPoints(config);

		List<InetSocketAddress> addresses = new LinkedList<>();
		for (String host : contactPoints.keySet()) {
			addresses.add(new InetSocketAddress(host, contactPoints.get(host)));
		}

		return addresses;
	}

	private PoolingOptions extractPoolingOptions(Map<String, String> config) throws ConfigurationException {
		PoolingOptions poolingOptions = new PoolingOptions();

		poolingOptions.setConnectionsPerHost(HostDistance.LOCAL,
				ConfigUtil.getPositiveIntegerValue(CassandraConfig.CONFIG_PROP_POOL_SIZE_LOCAL_MIN, config,
						CassandraConfig.POOL_SIZE_LOCAL_MIN_DEFAULT_VALUE),
				ConfigUtil.getPositiveIntegerValue(CassandraConfig.CONFIG_PROP_POOL_SIZE_LOCAL_MAX, config,
						CassandraConfig.POOL_SIZE_LOCAL_MAX_DEFAULT_VALUE));

		poolingOptions.setConnectionsPerHost(HostDistance.REMOTE,
				ConfigUtil.getPositiveIntegerValue(CassandraConfig.CONFIG_PROP_POOL_SIZE_REMOTE_MIN, config,
						CassandraConfig.POOL_SIZE_REMOTE_MIN_DEFAULT_VALUE),
				ConfigUtil.getPositiveIntegerValue(CassandraConfig.CONFIG_PROP_POOL_SIZE_REMOTE_MAX, config,
						CassandraConfig.POOL_SIZE_REMOTE_MAX_DEFAULT_VALUE));

		poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL,
				ConfigUtil.getPositiveIntegerValue(CassandraConfig.CONFIG_PROP_MAX_REQUESTS_LOCAL, config,
						CassandraConfig.MAX_REQUESTS_LOCAL_DEFAULT_VALUE));

		poolingOptions.setMaxRequestsPerConnection(HostDistance.REMOTE,
				ConfigUtil.getPositiveIntegerValue(CassandraConfig.CONFIG_PROP_MAX_REQUESTS_REMOTE, config,
						CassandraConfig.MAX_REQUESTS_REMOTE_DEFAULT_VALUE));

		return poolingOptions;
	}

}
