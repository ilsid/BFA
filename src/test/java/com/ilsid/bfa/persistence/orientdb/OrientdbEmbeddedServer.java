package com.ilsid.bfa.persistence.orientdb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.orientdb.OrientdbSystemException;
import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * Provides interface to embedded OrientDB server.
 * 
 * @author illia.sydorovych
 *
 */
public class OrientdbEmbeddedServer {

	private static final String CONFIG_FILE = TestConstants.TEST_RESOURCES_DIR + "/test-orientdb-config.xml";

	private static OServer server;

	/**
	 * Starts the server.
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
			try (InputStream is = new FileInputStream(CONFIG_FILE)) {
				server.startup(is);
			}
			server.activate();

			initDatabase();
		} catch (Exception e) {
			shutdown();
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

	private static void initDatabase() throws IOException {
		OrientGraphFactory factory = new OrientGraphFactory("remote:localhost/BFA_test", "root", "root").setupPool(1,
				10);

		ODatabaseDocumentTx db = factory.getDatabase();
		String script = FileUtils.readFileToString(new File("src/main/resources/database/schema.sql"));
		db.command(new OCommandScript("sql", script)).execute();

		factory.close();
	}

}
