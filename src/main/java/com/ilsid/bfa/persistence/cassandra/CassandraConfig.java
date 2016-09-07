package com.ilsid.bfa.persistence.cassandra;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.common.NumberUtil;

/**
 * Includes Cassandra related configuration properties, default values and specific configuration routines.
 * 
 * @author illia.sydorovych
 *
 */
public class CassandraConfig {

	public static final String CONFIG_PROP_CONTACT_POINTS = "bfa.persistence.cassandra.contact_points";

	public static final String CONFIG_PROP_POOL_SIZE_LOCAL_MIN = "bfa.persistence.cassandra.pool_size.local.min";

	public static final String CONFIG_PROP_POOL_SIZE_LOCAL_MAX = "bfa.persistence.cassandra.pool_size.local.max";

	public static final String CONFIG_PROP_POOL_SIZE_REMOTE_MIN = "bfa.persistence.cassandra.pool_size.remote.min";

	public static final String CONFIG_PROP_POOL_SIZE_REMOTE_MAX = "bfa.persistence.cassandra.pool_size.remote.max";

	public static final String CONFIG_PROP_MAX_REQUESTS_LOCAL = "bfa.persistence.cassandra.max_requests.local";

	public static final String CONFIG_PROP_MAX_REQUESTS_REMOTE = "bfa.persistence.cassandra.max_requests.remote";

	public static final int NATIVE_TRANSPORT_PORT_DEFAULT_VALUE = 9042;

	public static final int POOL_SIZE_LOCAL_MIN_DEFAULT_VALUE = 2;

	public static final int POOL_SIZE_LOCAL_MAX_DEFAULT_VALUE = 10;

	public static final int POOL_SIZE_REMOTE_MIN_DEFAULT_VALUE = 2;

	public static final int POOL_SIZE_REMOTE_MAX_DEFAULT_VALUE = 5;

	/*
	 * Max simultaneous requests per connection.
	 */
	public static final int MAX_REQUESTS_LOCAL_DEFAULT_VALUE = 2048;

	/*
	 * Max simultaneous requests per connection.
	 */
	public static final int MAX_REQUESTS_REMOTE_DEFAULT_VALUE = 512;

	private static final String CONTACT_POINT_SEPARATOR = ",";

	private static final String PORT_SEPARATOR = ":";

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
		String[] points = propValue.split(CONTACT_POINT_SEPARATOR);

		for (String point : points) {
			String[] pointParts = point.replace(StringUtils.SPACE, StringUtils.EMPTY).split(PORT_SEPARATOR, 2);
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
