package com.ilsid.bfa.persistence.cassandra;

import java.util.HashMap;
import java.util.Map;

import org.cassandraunit.utils.EmbeddedCassandraServerHelper;

public class CassandraEmbeddedServer {

	private static boolean serverStarted;

	private static Map<String, String> clientConfig;

	private static CassandraClient client;

	static {
		clientConfig = new HashMap<>();
		// CQL port is 9142 in cassandra-unit config. See <cassandra-unit jar>/cu-cassandra.yaml
		clientConfig.put(CassandraConfig.CONFIG_PROP_CONTACT_POINTS, "localhost:9142");
		clientConfig.put(CassandraConfig.CONFIG_PROP_CONNECTION_TIMEOUT, "10000");
	}

	public static synchronized void startup() throws Exception {
		if (serverStarted) {
			return;
		}

		final CassandraRunnable cassandraJob = new CassandraRunnable();
		final Thread ct = new Thread(cassandraJob);

		ct.start();
		ct.join();

		if (cassandraJob.startException != null) {
			throw cassandraJob.startException;
		}

		client = new CassandraClient();
		client.setConfiguration(clientConfig);

		client.initDatabase();

		serverStarted = true;
	}

	public static synchronized void shutdown() {
		if (serverStarted) {
			client.dropDatabase();
			serverStarted = false;
		}
	}

	public static Map<String, String> getClientConfig() {
		return clientConfig;
	}

	public static CassandraClient getClient() {
		return client;
	}

	private static class CassandraRunnable implements Runnable {

		private Exception startException;

		public void run() {
			try {
				EmbeddedCassandraServerHelper.startEmbeddedCassandra();
			} catch (Exception e) {
				startException = e;
			}

		}

	}

	public static void main(String[] args) throws Exception {
		startup();
	}

}
