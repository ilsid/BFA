package com.ilsid.bfa.persistence.cassandra;

import java.util.Map;

import com.ilsid.bfa.persistence.DatabaseServerManager;

public class CassandraServerManager implements DatabaseServerManager {

	public Map<String, String> getConfig() {
		return CassandraEmbeddedServer.getClientConfig();
	}

	public void startServer() throws Exception {
		CassandraEmbeddedServer.startup();
	}

	public void stopServer() {
		CassandraEmbeddedServer.shutdown();
	}

}
