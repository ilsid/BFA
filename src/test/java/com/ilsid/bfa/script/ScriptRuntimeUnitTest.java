package com.ilsid.bfa.script;

import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.ResultSet;
import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.persistence.cassandra.CassandraEmbeddedServer;
import com.ilsid.bfa.persistence.cassandra.CassandraRuntimeRepositoryInitializer;

public class ScriptRuntimeUnitTest extends BaseUnitTestCase {

	private static final Set<Object> uniqueRuntimeIds = new HashSet<>();

	private static ScriptRuntime runtime;

	@BeforeClass
	public static void beforeClass() throws Exception {
		CassandraEmbeddedServer.startup();
		ScriptingRepositoryInitializer.init();

		runtime = new ScriptRuntime();
		runtime.setRepository(CassandraRuntimeRepositoryInitializer.init());
	}

	@AfterClass
	public static void afterClass() throws Exception {
		ScriptingRepositoryInitializer.cleanup();
		CassandraEmbeddedServer.shutdown();
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

		ResultSet rs = CassandraEmbeddedServer.getClient()
				.queryWithAllowedFiltering("SELECT * FROM bfa.running_flows WHERE runtime_id=" + runtimeId);

		assertEquals(1, rs.all().size());
	}

	private void assertRuntimeId(Object value) {
		assertNotNull(value);
		assertTrue(uniqueRuntimeIds.add(value));
	}

}
