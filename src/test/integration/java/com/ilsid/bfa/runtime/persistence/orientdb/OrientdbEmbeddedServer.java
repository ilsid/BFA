package com.ilsid.bfa.runtime.persistence.orientdb;

import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

/**
 * Provides interface to embedded OrientDB server.
 * 
 * @author illia.sydorovych
 *
 */
public class OrientdbEmbeddedServer {

	private static final String CONFIG_FILE = "test-orientdb-config.xml";

	private static OServer server;

	/**
	 * Starts the server. The <i>ORIENTDB_HOME</i> system property must be set before the startup.
	 * 
	 * @throws OrientdbSystemException
	 *             if the server can't be started
	 * @throws IllegalStateException
	 *             if the server has been already started
	 */
	public static synchronized void startup() throws OrientdbSystemException, IllegalStateException {
		if (server != null) {
			throw new IllegalStateException("Embedded OrientDB server has been already started");
		}

		try {
			server = OServerMain.create();
			server.startup(Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE));
			server.activate();
		} catch (Exception e) {
			throw new OrientdbSystemException("Startup of embedded OrientDB server failed", e);
		}
	}

	/**
	 * Stops the server, if it was started. Does nothing otherwise.
	 */
	public static synchronized void shutdown() {
		if (server != null) {
			server.shutdown();
			server = null;
		}
	}

}
