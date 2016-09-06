package com.ilsid.bfa.persistence.orientdb;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.ilsid.bfa.IntegrationTestConstants;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.DatabaseServerManager;

public class OrientdbServerManager implements DatabaseServerManager {

	public Map<String, String> getConfig() {
		Map<String, String> config = new HashMap<>();
		config.put("bfa.persistence.orientdb.url", TestConstants.DATABASE_URL);
		config.put("bfa.persistence.orientdb.user", TestConstants.DATABASE_USER);
		config.put("bfa.persistence.orientdb.password", TestConstants.DATABASE_PASSWORD);
		
		return config;
	}
	
	public void startServer() throws Exception {
		// Copy initial clean database to the testing directory
		FileUtils.copyDirectory(TestConstants.INIT_DATABASE_DIR, IntegrationTestConstants.DATABASE_DIR);
		System.setProperty(TestConstants.ORIENTDB_HOME_PROPERTY, IntegrationTestConstants.ORIENTDB_HOME_DIR.getPath());
		OrientdbEmbeddedServer.startup();
	}

	public void stopServer() {
		System.getProperties().remove(TestConstants.ORIENTDB_HOME_PROPERTY);
		OrientdbEmbeddedServer.shutdown();
	}

}
