package com.ilsid.bfa.runtime.persistence.cassandra;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.persistence.cassandra.CassandraEmbeddedServer;
import com.ilsid.bfa.persistence.cassandra.CassandraRuntimeRepositoryInitializer;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

public class CassandraRuntimeRepositoryUnitTest extends BaseUnitTestCase {

	private static final LinkedList<String> EMPTY_LIST = new LinkedList<>();

	private static final SimpleDateFormat TOKEN_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private static final long ONE_MINUTE_IN_MILLIS = 60000;

	private static RuntimeRepository repository;

	@BeforeClass
	public static void beforeClass() throws Exception {
		CassandraEmbeddedServer.startup();
		repository = CassandraRuntimeRepositoryInitializer.init();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		CassandraEmbeddedServer.shutdown();
	}

	@Test
	public void failedFlowsCanBeFetched() throws Exception {
		final Object runtimeId = repository.getNextRuntimeId();
		final Date startTime = new Date();
		final Date endTime = startTime;
		final String startDate = TOKEN_DATE_FORMAT.format(startTime);

		CassandraEmbeddedServer.getClient().executeBoundStatement(CassandraRuntimeRepository.FAILED_FLOWS_INSERT_STMT,
				runtimeId, "system", "Test Script", EMPTY_LIST, startDate, startTime, EMPTY_LIST, endTime,
				getErrorDetails());

		final Object secondRuntimeId = repository.getNextRuntimeId();
		// next start time = start time + 5 mins
		final Date secondStartTime = new Date(startTime.getTime() + 5 * ONE_MINUTE_IN_MILLIS);
		final Date secondEndTime = secondStartTime;

		CassandraEmbeddedServer.getClient().executeBoundStatement(CassandraRuntimeRepository.FAILED_FLOWS_INSERT_STMT,
				secondRuntimeId, "system", "Test Script 2", getParameters(), startDate, secondStartTime, EMPTY_LIST,
				secondEndTime, getErrorDetails());

		final Object thirdRuntimeId = repository.getNextRuntimeId();
		// next start time = start time + 5 mins
		final Date thirdStartTime = new Date(secondStartTime.getTime() + 5 * ONE_MINUTE_IN_MILLIS);
		final Date thirdEndTime = thirdStartTime;

		CassandraEmbeddedServer.getClient().executeBoundStatement(CassandraRuntimeRepository.FAILED_FLOWS_INSERT_STMT,
				thirdRuntimeId, "system", "Test Script 3", EMPTY_LIST, startDate, thirdStartTime, EMPTY_LIST,
				thirdEndTime, getErrorDetails());

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.FAILED).setStartDate(startTime),
				new QueryPagingOptions());

		final List<ScriptRuntimeDTO> records = fetchResult.getResult();
		
		assertNull(fetchResult.getNextPageToken());
		assertEquals(3, records.size());
		
		// records are returned by startTime DESC
		assertEquals(thirdStartTime, records.get(0).getStartTime());
		assertEquals("Test Script 3", records.get(0).getScriptName());
		assertTrue(records.get(0).getParameters().isEmpty());

		assertEquals(secondStartTime, records.get(1).getStartTime());
		assertEquals("Test Script 2", records.get(1).getScriptName());
		assertEquals(getParameters(), records.get(1).getParameters());

		assertEquals(startTime, records.get(2).getStartTime());
		assertEquals("Test Script", records.get(2).getScriptName());
		assertTrue(records.get(2).getParameters().isEmpty());
	}

	@SuppressWarnings("serial")
	private List<String> getErrorDetails() {
		return new LinkedList<String>() {
			{
				add("Flow error");
				add("Root error");
			}
		};
	}

	@SuppressWarnings("serial")
	private List<String> getParameters() {
		return new LinkedList<String>() {
			{
				add("Param 1");
				add("Param 2");
			}
		};
	}

}
