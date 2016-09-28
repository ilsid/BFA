package com.ilsid.bfa.runtime.persistence.cassandra;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.ExceptionUtil;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.persistence.cassandra.CassandraEmbeddedServer;
import com.ilsid.bfa.persistence.cassandra.CassandraRuntimeRepositoryInitializer;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

public class CassandraRuntimeRepositoryUnitTest extends BaseUnitTestCase {

	private static final String USER_NAME = "some user";

	private static final LinkedList<String> EMPTY_LIST = new LinkedList<>();

	private static final SimpleDateFormat TOKEN_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

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
	
	@After
	public void afterTest() {
		clearFailedFlows();
	}

	@Test
	public void allFailedFlowFieldsAreFetched() throws Exception {
		final Date startTime = new Date();
		insertFailedFlows(1, startTime, getParameters(), getCallStack(), getErrorDetails());

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.FAILED).setStartDate(startTime),
				new QueryPagingOptions());

		final List<ScriptRuntimeDTO> records = fetchResult.getResult();

		assertNull(fetchResult.getNextPageToken());
		assertEquals(1, records.size());

		ScriptRuntimeDTO rec = records.get(0);
		
		assertNotNull(rec.getRuntimeId());;
		assertEquals(USER_NAME, rec.getUserName());
		assertEquals("Test Script 1", rec.getScriptName());
		assertEquals(getParameters(), rec.getParameters());
		assertEquals(addMinutes(startTime, 1), rec.getStartTime());
		assertEquals(addMinutes(startTime, 1), rec.getEndTime());
		assertEquals(getCallStack(), rec.getCallStack());
		assertEquals(ExceptionUtil.toException(getErrorDetails()).getMessage(), rec.getError().getMessage());
	}

	@Test
	public void failedFlowsAreFetchedByDescendingStartTimeOrder() throws Exception {
		final Date startTime = new Date();
		insertFailedFlows(3, startTime);

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.FAILED).setStartDate(startTime),
				new QueryPagingOptions());

		final List<ScriptRuntimeDTO> records = fetchResult.getResult();

		assertNull(fetchResult.getNextPageToken());
		assertEquals(3, records.size());

		assertEquals(addMinutes(startTime, 3), records.get(0).getStartTime());
		assertEquals("Test Script 3", records.get(0).getScriptName());

		assertEquals(addMinutes(startTime, 2), records.get(1).getStartTime());
		assertEquals("Test Script 2", records.get(1).getScriptName());

		assertEquals(addMinutes(startTime, 1), records.get(2).getStartTime());
		assertEquals("Test Script 1", records.get(2).getScriptName());
	}

	@Test
	public void fetchedFlowsResultIsPaginated() throws Exception {
		final Date startTime = new Date();
		final int totalRecords = 100;
		final int pageSize = 80;

		insertFailedFlows(totalRecords, startTime);
		
		final QueryPagingOptions pagingOptions = new QueryPagingOptions().setResultsPerPage(pageSize);
		
		QueryPage<ScriptRuntimeDTO> firstPage = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.FAILED).setStartDate(startTime),
				pagingOptions);
		
		final String nextPageToken = firstPage.getNextPageToken();
		assertNotNull(nextPageToken);
		assertEquals(pageSize, firstPage.getResult().size());
		
		QueryPage<ScriptRuntimeDTO> secondPage = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.FAILED).setStartDate(startTime),
				pagingOptions.setPageToken(nextPageToken));
		
		assertNull(secondPage.getNextPageToken());
		assertEquals(totalRecords - pageSize, secondPage.getResult().size());
	}

	@SuppressWarnings("serial")
	private List<String> getCallStack() {
		return new LinkedList<String>() {
			{
				add("Flow B");
				add("Flow A");
			}
		};
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

	private void insertFailedFlows(int recordsCount, final Date initTime) {
		insertFailedFlows(recordsCount, initTime, EMPTY_LIST, EMPTY_LIST, EMPTY_LIST);
	}

	private void insertFailedFlows(int recordsCount, final Date initTime, List<String> parameters,
			List<String> callStack, List<String> errorDetails) {

		final String startDate = TOKEN_DATE_FORMAT.format(initTime);

		for (int cnt = 1; cnt < recordsCount + 1; cnt++) {
			Date startTime = addMinutes(initTime, cnt);
			Date endTime = startTime;

			CassandraEmbeddedServer.getClient().executeBoundStatement(
					CassandraRuntimeRepository.FAILED_FLOWS_INSERT_STMT, UUID.randomUUID(), USER_NAME,
					"Test Script " + cnt, parameters, startDate, startTime, callStack, endTime, errorDetails);

		}
	}
	
	private void clearFailedFlows() {
		CassandraEmbeddedServer.getClient().query("TRUNCATE failed_flows");
	}

}
