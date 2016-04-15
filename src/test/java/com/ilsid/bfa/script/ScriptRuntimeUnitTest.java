package com.ilsid.bfa.script;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.orientdb.OrientdbEmbeddedServer;

public class ScriptRuntimeUnitTest extends BaseUnitTestCase {

	private final static File ORIENTDB_HOME_DIR = new File(REPOSITORY_ROOT_DIR, "orientdb");

	private final static File DATABASE_DIR = new File(ORIENTDB_HOME_DIR, "databases");

	private static ScriptRuntime runtime;
	
	private static final Set<Long> uniqueRuntimeIds = new HashSet<>();

	@BeforeClass
	public static void beforeClass() throws Exception {
		startDatabaseServer();

		CodeRepositoryInitializer.init();

		runtime = new ScriptRuntime();
		runtime.setRepository(RuntimeRepositoryInitializer.init());
	}

	@AfterClass
	public static void afterClass() throws Exception {
		stopDatabaseServer();
	}

	@Test
	public void scriptCanBeRun() throws Exception {
		long runtimeId = runtime.runScript("Script001");
		assertRuntimeId(runtimeId);
	}

	@Test
	public void scriptWithSubflowCanBeRun() throws Exception {
		long runtimeId = runtime.runScript("SingleSubflowScript");
		assertRuntimeId(runtimeId);
	}
	
	private void assertRuntimeId(long value) {
		assertTrue(value > 0);
		assertTrue(uniqueRuntimeIds.add(value));
	}

	private static void startDatabaseServer() throws Exception {
		// Copy initial clean database to the testing directory
		FileUtils.copyDirectory(TestConstants.INIT_DATABASE_DIR, DATABASE_DIR);
		System.setProperty(TestConstants.ORIENTDB_HOME_PROPERTY, ORIENTDB_HOME_DIR.getPath());
		OrientdbEmbeddedServer.startup();
	}

	private static void stopDatabaseServer() throws Exception {
		System.getProperties().remove(TestConstants.ORIENTDB_HOME_PROPERTY);
		OrientdbEmbeddedServer.shutdown();
		FileUtils.forceDelete(REPOSITORY_ROOT_DIR);
	}

}
