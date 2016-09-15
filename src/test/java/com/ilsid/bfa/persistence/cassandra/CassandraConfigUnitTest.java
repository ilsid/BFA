package com.ilsid.bfa.persistence.cassandra;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.SocketOptions;
import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.ConfigurationException;

public class CassandraConfigUnitTest extends BaseUnitTestCase {

	private static final Map<String, String> EMPTY_CONFIG = Collections.<String, String> emptyMap();

	private static final String[] PO_NAMES = new String[] { CassandraConfig.CONFIG_PROP_POOL_SIZE_LOCAL_MIN,
			CassandraConfig.CONFIG_PROP_POOL_SIZE_LOCAL_MAX, CassandraConfig.CONFIG_PROP_POOL_SIZE_REMOTE_MIN,
			CassandraConfig.CONFIG_PROP_POOL_SIZE_REMOTE_MAX, CassandraConfig.CONFIG_PROP_MAX_REQUESTS_LOCAL,
			CassandraConfig.CONFIG_PROP_MAX_REQUESTS_REMOTE };

	@Test
	public void singleContactPointHostCanBeExtracted() throws Exception {
		Map<String, Integer> points = extractContactPoints("localhost");

		assertEquals(1, points.size());
		assertEquals(CassandraConfig.NATIVE_TRANSPORT_PORT_DEFAULT_VALUE, points.get("localhost"));
	}

	@Test
	public void singleContactPointHostAndPortCanBeExtracted() throws Exception {
		Map<String, Integer> points = extractContactPoints("localhost:8888");

		assertEquals(1, points.size());
		assertEquals(8888, points.get("localhost"));
	}

	@Test
	public void severalContactPointHostsCanBeExtracted() throws Exception {
		// Note, spaces are allowed, trailing commas are allowed
		Map<String, Integer> points = extractContactPoints("host1,host2,  host3, ");

		assertEquals(3, points.size());
		assertEquals(CassandraConfig.NATIVE_TRANSPORT_PORT_DEFAULT_VALUE, points.get("host1"));
		assertEquals(CassandraConfig.NATIVE_TRANSPORT_PORT_DEFAULT_VALUE, points.get("host2"));
		assertEquals(CassandraConfig.NATIVE_TRANSPORT_PORT_DEFAULT_VALUE, points.get("host3"));
	}

	@Test
	public void severalContactPointHostPortPairsCanBeExtracted() throws Exception {
		// Note, spaces are allowed
		Map<String, Integer> points = extractContactPoints("host1:8888,host2 :9999, host3");

		assertEquals(3, points.size());
		assertEquals(8888, points.get("host1"));
		assertEquals(9999, points.get("host2"));
		assertEquals(CassandraConfig.NATIVE_TRANSPORT_PORT_DEFAULT_VALUE, points.get("host3"));
	}

	@Test
	public void contactPointWithInvalidPortCanNotBeExtracted() throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule.expectMessage(
				String.format("Failed to parse the configuration property [%s]. [88ab] is not a valid port number",
						CassandraConfig.CONFIG_PROP_CONTACT_POINTS));

		extractContactPoints("localhost:88ab");
	}

	@Test
	public void contactPointIsRequiredProperty() throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule.expectMessage(String.format("The required configuration property [%s] is missed",
				CassandraConfig.CONFIG_PROP_CONTACT_POINTS));

		CassandraConfig.extractContactPoints(EMPTY_CONFIG);
	}

	@Test
	public void poolingOptionsCanBeExtracted() throws Exception {
		assertPoolingOptionNames();

		@SuppressWarnings("serial")
		PoolingOptions options = extractPoolingOptions(new HashMap<String, String>() {
			{
				put(PO_NAMES[0], "5");
				put(PO_NAMES[1], "20");
				put(PO_NAMES[2], "3");
				put(PO_NAMES[3], "10");
				put(PO_NAMES[4], "1000");
				put(PO_NAMES[5], "500");
			}
		});

		assertEquals(5, options.getCoreConnectionsPerHost(HostDistance.LOCAL));
		assertEquals(20, options.getMaxConnectionsPerHost(HostDistance.LOCAL));
		assertEquals(3, options.getCoreConnectionsPerHost(HostDistance.REMOTE));
		assertEquals(10, options.getMaxConnectionsPerHost(HostDistance.REMOTE));
		assertEquals(1000, options.getMaxRequestsPerConnection(HostDistance.LOCAL));
		assertEquals(500, options.getMaxRequestsPerConnection(HostDistance.REMOTE));
	}

	@Test
	public void defaultPoolingOptionsAreExtractedIfNotSet() throws Exception {
		assertPoolingOptionNames();

		PoolingOptions options = extractPoolingOptions(EMPTY_CONFIG);

		assertEquals(CassandraConfig.PO_DEFAULTS.get(PO_NAMES[0]),
				options.getCoreConnectionsPerHost(HostDistance.LOCAL));
		assertEquals(CassandraConfig.PO_DEFAULTS.get(PO_NAMES[1]),
				options.getMaxConnectionsPerHost(HostDistance.LOCAL));
		assertEquals(CassandraConfig.PO_DEFAULTS.get(PO_NAMES[2]),
				options.getCoreConnectionsPerHost(HostDistance.REMOTE));
		assertEquals(CassandraConfig.PO_DEFAULTS.get(PO_NAMES[3]),
				options.getMaxConnectionsPerHost(HostDistance.REMOTE));
		assertEquals(CassandraConfig.PO_DEFAULTS.get(PO_NAMES[4]),
				options.getMaxRequestsPerConnection(HostDistance.LOCAL));
		assertEquals(CassandraConfig.PO_DEFAULTS.get(PO_NAMES[5]),
				options.getMaxRequestsPerConnection(HostDistance.REMOTE));
	}

	@Test
	public void poolingOptionsWithStringValuesCanNotBeExtracted() throws Exception {
		assertPoolingOptionExceptions("abc");
	}

	@Test
	public void poolingOptionsWithZeroValuesCanNotBeExtracted() throws Exception {
		assertPoolingOptionExceptions("0");
	}

	@Test
	public void poolingOptionsWithNegativeValuesCanNotBeExtracted() throws Exception {
		assertPoolingOptionExceptions("-55");
	}

	@Test
	public void connectionTimeoutCanBeExtracted() throws Exception {
		assertEquals(10000, extractConnectionTimeout("10000"));
	}

	@Test
	public void defaultConnectionTimeoutIsReturnedIfNotSet() throws Exception {
		assertEquals(SocketOptions.DEFAULT_CONNECT_TIMEOUT_MILLIS, extractConnectionTimeout(null));
	}

	@Test
	public void connectionTimeoutWithStringValuesCanNotBeExtracted() throws Exception {
		assertConnectionTimeoutException("abc");
	}

	@Test
	public void connectionTimeoutWithZeroValuesCanNotBeExtracted() throws Exception {
		assertConnectionTimeoutException("0");
	}

	@Test
	public void connectionTimeoutWithNegativeValuesCanNotBeExtracted() throws Exception {
		assertConnectionTimeoutException("-55");
	}

	@SuppressWarnings("serial")
	private Map<String, String> getConfigWithContactPoints(final String value) {
		return new HashMap<String, String>() {
			{
				put(CassandraConfig.CONFIG_PROP_CONTACT_POINTS, value);
			}
		};
	}

	@SuppressWarnings("serial")
	private Map<String, String> getConfigWithConnectionTimeout(final String value) {
		return new HashMap<String, String>() {
			{
				put(CassandraConfig.CONFIG_PROP_CONNECTION_TIMEOUT, value);
			}
		};
	}

	private Map<String, Integer> extractContactPoints(String value) throws Exception {
		return CassandraConfig.extractContactPoints(getConfigWithContactPoints(value));
	}

	private int extractConnectionTimeout(String value) throws Exception {
		if (value == null) {
			return CassandraConfig.extractConnectionTimeout(new HashMap<String, String>());
		} else {
			return CassandraConfig.extractConnectionTimeout(getConfigWithConnectionTimeout(value));
		}
	}

	private PoolingOptions extractPoolingOptions(Map<String, String> config) throws Exception {
		return CassandraConfig.extractPoolingOptions(config);
	}

	@SuppressWarnings("serial")
	private void assertPoolingOptionException(final String name, final String value) throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule.expectMessage(
				String.format("The value of the configuration property [%s] must be a positive integer", name));

		extractPoolingOptions(new HashMap<String, String>() {
			{
				put(name, value);
			}
		});
	}

	private void assertPoolingOptionExceptions(final String value) throws Exception {
		assertPoolingOptionNames();

		for (String optionName : PO_NAMES) {
			assertPoolingOptionException(optionName, value);
		}
	}

	private void assertPoolingOptionNames() {
		assertTrue(Arrays.equals(PO_NAMES, CassandraConfig.PO_DEFAULTS.keySet().toArray(new String[] {})));
	}

	private void assertConnectionTimeoutException(final String value) throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule
				.expectMessage(String.format("The value of the configuration property [%s] must be a positive integer",
						CassandraConfig.CONFIG_PROP_CONNECTION_TIMEOUT));

		extractConnectionTimeout(value);
	}

}
