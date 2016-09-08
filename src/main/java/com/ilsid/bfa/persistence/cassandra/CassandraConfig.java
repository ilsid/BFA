package com.ilsid.bfa.persistence.cassandra;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.ConfigUtil;
import com.ilsid.bfa.common.NumberUtil;

/**
 * Includes Cassandra related configuration properties, default values and specific configuration routines.
 * 
 * @author illia.sydorovych
 *
 */
public class CassandraConfig {

	static final String CONFIG_PROP_CONTACT_POINTS = "bfa.persistence.cassandra.contact_points";

	static final String CONFIG_PROP_POOL_SIZE_LOCAL_MIN = "bfa.persistence.cassandra.pool_size.local.min";

	static final String CONFIG_PROP_POOL_SIZE_LOCAL_MAX = "bfa.persistence.cassandra.pool_size.local.max";

	static final String CONFIG_PROP_POOL_SIZE_REMOTE_MIN = "bfa.persistence.cassandra.pool_size.remote.min";

	static final String CONFIG_PROP_POOL_SIZE_REMOTE_MAX = "bfa.persistence.cassandra.pool_size.remote.max";

	/*
	 * Max simultaneous requests per connection.
	 */
	static final String CONFIG_PROP_MAX_REQUESTS_LOCAL = "bfa.persistence.cassandra.max_requests.local";

	/*
	 * Max simultaneous requests per connection.
	 */
	static final String CONFIG_PROP_MAX_REQUESTS_REMOTE = "bfa.persistence.cassandra.max_requests.remote";

	static final int NATIVE_TRANSPORT_PORT_DEFAULT_VALUE = 9042;

	static final Map<String, Integer> PO_DEFAULTS;

	private static final String CONTACT_POINT_SEPARATOR = ",";

	private static final String PORT_SEPARATOR = ":";

	static {
		PO_DEFAULTS = new LinkedHashMap<String, Integer>();

		PO_DEFAULTS.put(CONFIG_PROP_POOL_SIZE_LOCAL_MIN, 2);
		PO_DEFAULTS.put(CONFIG_PROP_POOL_SIZE_LOCAL_MAX, 10);
		PO_DEFAULTS.put(CONFIG_PROP_POOL_SIZE_REMOTE_MIN, 2);
		PO_DEFAULTS.put(CONFIG_PROP_POOL_SIZE_REMOTE_MAX, 5);
		PO_DEFAULTS.put(CONFIG_PROP_MAX_REQUESTS_LOCAL, 2048);
		PO_DEFAULTS.put(CONFIG_PROP_MAX_REQUESTS_REMOTE, 512);
	}

	/**
	 * Extracts contact points from the configuration. {@link #CONFIG_PROP_CONTACT_POINTS} property is expected to be in
	 * the configuration. The examples of valid contacts points are:
	 * 
	 * <pre>
	 * <code>
	 * localhost
	 * localhost:9042
	 * localhost:9042,another_host
	 * localhost:9042,another_host:9042
	 * localhost,another_host:9042,yet_another_host
	 * </code>
	 * </pre>
	 * 
	 * If a port is not explicitly defined for any host then {@link #NATIVE_TRANSPORT_PORT_DEFAULT_VALUE} is used.
	 * 
	 * @param config
	 *            configuration
	 * @return a contact points map, where the key is a host name and the value is a port number.
	 * @throws ConfigurationException
	 *             if the given configuration does not contain {@link #CONFIG_PROP_CONTACT_POINTS} property or this
	 *             property has invalid format
	 */
	public static Map<String, Integer> extractContactPoints(Map<String, String> config) throws ConfigurationException {

		String propValue = config.get(CONFIG_PROP_CONTACT_POINTS);
		if (propValue == null) {
			throw new ConfigurationException(
					String.format("The required configuration property [%s] is missed", CONFIG_PROP_CONTACT_POINTS));
		}

		Map<String, Integer> contactPoints = new LinkedHashMap<>();
		String[] points = propValue.replace(StringUtils.SPACE, StringUtils.EMPTY).split(CONTACT_POINT_SEPARATOR);

		for (String point : points) {
			String[] pointParts = point.split(PORT_SEPARATOR, 2);
			final String host = pointParts[0];

			if (pointParts.length > 1) {
				final int port = getPort(pointParts[1]);
				contactPoints.put(host, port);
			} else {
				contactPoints.put(host, NATIVE_TRANSPORT_PORT_DEFAULT_VALUE);
			}
		}

		return contactPoints;
	}

	/**
	 * Extract pooling options from the configuration. Sets default values for missing options.
	 * 
	 * @param config
	 *            configuration
	 * @return the pooling options
	 * @throws ConfigurationException
	 *             if pooling options have invalid format
	 */
	public static PoolingOptions extractPoolingOptions(Map<String, String> config) throws ConfigurationException {
		PoolingOptions poolingOptions = new PoolingOptions();

		poolingOptions.setConnectionsPerHost(HostDistance.LOCAL,
				ConfigUtil.getPositiveIntegerValue(CONFIG_PROP_POOL_SIZE_LOCAL_MIN, config,
						PO_DEFAULTS.get(CONFIG_PROP_POOL_SIZE_LOCAL_MIN)),
				ConfigUtil.getPositiveIntegerValue(CONFIG_PROP_POOL_SIZE_LOCAL_MAX, config,
						PO_DEFAULTS.get(CONFIG_PROP_POOL_SIZE_LOCAL_MAX)));

		poolingOptions.setConnectionsPerHost(HostDistance.REMOTE,
				ConfigUtil.getPositiveIntegerValue(CONFIG_PROP_POOL_SIZE_REMOTE_MIN, config,
						PO_DEFAULTS.get(CONFIG_PROP_POOL_SIZE_REMOTE_MIN)),
				ConfigUtil.getPositiveIntegerValue(CONFIG_PROP_POOL_SIZE_REMOTE_MAX, config,
						PO_DEFAULTS.get(CONFIG_PROP_POOL_SIZE_REMOTE_MAX)));

		poolingOptions.setMaxRequestsPerConnection(HostDistance.LOCAL, ConfigUtil.getPositiveIntegerValue(
				CONFIG_PROP_MAX_REQUESTS_LOCAL, config, PO_DEFAULTS.get(CONFIG_PROP_MAX_REQUESTS_LOCAL)));

		poolingOptions.setMaxRequestsPerConnection(HostDistance.REMOTE, ConfigUtil.getPositiveIntegerValue(
				CONFIG_PROP_MAX_REQUESTS_REMOTE, config, PO_DEFAULTS.get(CONFIG_PROP_MAX_REQUESTS_REMOTE)));

		return poolingOptions;
	}

	private static int getPort(String portString) throws ConfigurationException {
		int port;

		if (NumberUtil.isPositiveInteger(portString)) {
			port = Integer.parseInt(portString);
		} else {
			throw new ConfigurationException(
					String.format("Failed to parse the configuration property [%s]. [%s] is not a valid port number",
							CONFIG_PROP_CONTACT_POINTS, portString));
		}

		return port;
	}

}
