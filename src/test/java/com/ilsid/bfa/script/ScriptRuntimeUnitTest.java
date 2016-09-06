package com.ilsid.bfa.script;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.persistence.cassandra.CassandraEmbeddedServer;
import com.ilsid.bfa.persistence.cassandra.CassandraRuntimeRepositoryInitializer;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;

public class ScriptRuntimeUnitTest extends BaseUnitTestCase {

	private static final String RUNTIME_ID_COLUMN = "runtime_id";

	private static final String RUNNING_FLOWS_QUERY_TPLT = "SELECT * FROM bfa.running_flows WHERE runtime_id=%s";

	// Filtering by status is needed here to avoid
	// [PRIMARY KEY column "runtime_id" cannot be restricted as preceding column "status" is not restricted] error
	private static final String COMPLETED_FLOWS_BY_DATE_QUERY_TPLT = "SELECT * FROM bfa.completed_flows_by_date WHERE status='Completed' AND runtime_id=%s";

	private static final String COMPLETED_FLOWS_BY_STATUS_QUERY_TPLT = "SELECT * FROM bfa.completed_flows_by_status WHERE runtime_id=%s";

	private static final SimpleDateFormat TOKEN_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

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
		assertSingleRecordIsPersisted(String.format(RUNNING_FLOWS_QUERY_TPLT, runtimeId), runtimeId);
		assertSingleRecordIsPersisted(String.format(COMPLETED_FLOWS_BY_DATE_QUERY_TPLT, runtimeId), runtimeId);
		assertSingleRecordIsPersisted(String.format(COMPLETED_FLOWS_BY_STATUS_QUERY_TPLT, runtimeId), runtimeId);
	}

	@Test
	public void runtimeRecordIsGeneratedForScriptWithSubflow() throws Exception {
		Object runtimeId = runtime.runScript("SingleSubflowScript");

		assertRuntimeId(runtimeId);
		assertTwoRecordsArePersisted(String.format(RUNNING_FLOWS_QUERY_TPLT, runtimeId), runtimeId);
		assertTwoRecordsWithValidDetailsArePersisted(String.format(COMPLETED_FLOWS_BY_DATE_QUERY_TPLT, runtimeId),
				runtimeId);
		assertTwoRecordsWithValidDetailsArePersisted(String.format(COMPLETED_FLOWS_BY_STATUS_QUERY_TPLT, runtimeId),
				runtimeId);
	}

	private void assertRuntimeId(Object value) {
		assertNotNull(value);
		assertTrue(uniqueRuntimeIds.add(value));
	}

	private void assertSingleRecordIsPersisted(String query, Object runtimeId) {
		ResultSet rs = CassandraEmbeddedServer.getClient().queryWithAllowedFiltering(query);

		final List<Row> rows = rs.all();
		assertEquals(1, rows.size());
		final Row row = rows.get(0);
		assertEquals(runtimeId, row.getUUID(RUNTIME_ID_COLUMN));
		Date startTime = row.getTimestamp("start_time");
		assertNotNull(startTime);
		assertEquals(row.getString("start_date"), TOKEN_DATE_FORMAT.format(startTime));
	}

	private void assertTwoRecordsArePersisted(String query, Object runtimeId) {
		ResultSet rs = CassandraEmbeddedServer.getClient().queryWithAllowedFiltering(query);

		final List<Row> rows = rs.all();
		assertEquals(2, rows.size());

		assertEquals(runtimeId, rows.get(0).getUUID(RUNTIME_ID_COLUMN));
		assertEquals(runtimeId, rows.get(1).getUUID(RUNTIME_ID_COLUMN));
	}

	private void assertTwoRecordsWithValidDetailsArePersisted(String query, Object runtimeId) {
		ResultSet rs = CassandraEmbeddedServer.getClient().queryWithAllowedFiltering(query);

		final List<Row> rows = rs.all();

		assertEquals(2, rows.size());
		// Records are ordered by start_time DESC
		final Row topScript = rows.get(1);
		final Row subflow = rows.get(0);

		assertEquals(runtimeId, topScript.getUUID(RUNTIME_ID_COLUMN));
		assertEquals(runtimeId, subflow.getUUID(RUNTIME_ID_COLUMN));

		assertTrue(topScript.getList("call_stack", String.class).isEmpty());
		assertEquals(RuntimeStatusType.COMPLETED.getValue(), topScript.getString("status"));
		final Date topScriptStartTime = topScript.getTimestamp("start_time");
		assertNotNull(topScriptStartTime);
		assertEquals(topScript.getString("start_date"), TOKEN_DATE_FORMAT.format(topScriptStartTime));
		assertNotNull(topScript.getTimestamp("end_time"));

		final List<String> subflowCallstack = subflow.getList("call_stack", String.class);
		assertEquals(1, subflowCallstack.size());
		// Call stack contains name of calling script
		assertEquals("SingleSubflowScript", subflowCallstack.get(0));
		assertEquals(RuntimeStatusType.COMPLETED.getValue(), subflow.getString("status"));
		final Date subflowStartTime = subflow.getTimestamp("start_time");
		assertNotNull(subflowStartTime);
		assertEquals(subflow.getString("start_date"), TOKEN_DATE_FORMAT.format(subflowStartTime));
		assertNotNull(subflow.getTimestamp("end_time"));
	}

}
