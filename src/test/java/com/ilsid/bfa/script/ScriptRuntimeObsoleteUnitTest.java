package com.ilsid.bfa.script;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.orientdb.OrientdbEmbeddedServer;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.ilsid.bfa.persistence.orientdb.OrientdbClient;

@Ignore
public class ScriptRuntimeObsoleteUnitTest extends BaseUnitTestCase {

	private final static File ORIENTDB_HOME_DIR = new File(REPOSITORY_ROOT_DIR, "orientdb");

	private final static File DATABASE_DIR = new File(ORIENTDB_HOME_DIR, "databases");

	private static ScriptRuntime runtime;

	private static final Set<Object> uniqueRuntimeIds = new HashSet<>();

	@BeforeClass
	public static void beforeClass() throws Exception {
		startDatabaseServer();
		OrientdbClient.init();

		ScriptingRepositoryInitializer.init();

		runtime = new ScriptRuntime();
		runtime.setRepository(RuntimeRepositoryInitializer.init());
	}

	@AfterClass
	public static void afterClass() throws Exception {
		OrientdbClient.release();
		stopDatabaseServer();
		ScriptingRepositoryInitializer.cleanup();
	}

	@Test
	public void scriptCanBeRun() throws Exception {
		Object runtimeId = runtime.runScript("Script001");
		assertRuntimeId(runtimeId);
	}

	@Test
	public void scriptWithSubflowCanBeRun() throws Exception {
		Object runtimeId = runtime.runScript("SingleSubflowScript");
		assertRuntimeId(runtimeId);
	}

	@Test
	public void runtimeRecordIsGenerated() throws Exception {
		Object runtimeId = runtime.runScript("Script001");
		assertRuntimeId(runtimeId);

		List<ODocument> records = OrientdbClient.query("SELECT * FROM FlowRuntime WHERE runtimeId=" + runtimeId);
		assertEquals(1, records.size());
		ODocument rec = records.get(0);

		assertEquals("Script001", rec.field("scriptName"));
		assertEquals(RuntimeStatusType.COMPLETED.getValue(), rec.field("status"));
		assertNotNull(rec.field("startTime"));
		assertNotNull(rec.field("endTime"));
	}

	@Test
	public void runtimeRecordIsGeneratedForScriptWithSubflow() throws Exception {
		Object runtimeId = runtime.runScript("SingleSubflowScript");
		assertRuntimeId(runtimeId);

		List<ODocument> records = OrientdbClient
				.query("SELECT * FROM FlowRuntime WHERE runtimeId=" + runtimeId + " ORDER BY @RID");
		assertEquals(2, records.size());
		ODocument topScript = records.get(0);
		ODocument subFlow = records.get(1);

		assertEquals("SingleSubflowScript", topScript.field("scriptName"));
		assertEquals(RuntimeStatusType.COMPLETED.getValue(), topScript.field("status"));
		assertNotNull(topScript.field("startTime"));
		assertNotNull(topScript.field("endTime"));
		assertNull(topScript.field("callStack"));

		assertEquals("Script001", subFlow.field("scriptName"));
		assertEquals(RuntimeStatusType.COMPLETED.getValue(), subFlow.field("status"));
		assertNotNull(subFlow.field("startTime"));
		assertNotNull(subFlow.field("endTime"));

		@SuppressWarnings("unchecked")
		final List<String> callStack = (List<String>) subFlow.field("callStack");
		assertEquals(1, callStack.size());
		// Subflow's call stack contains names of calling scripts (a single script in this case)
		assertEquals("SingleSubflowScript", callStack.get(0));
	}

	private void assertRuntimeId(Object value) {
		assertNotNull(value);
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
