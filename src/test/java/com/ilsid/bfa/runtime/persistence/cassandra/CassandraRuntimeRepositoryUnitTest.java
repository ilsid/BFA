package com.ilsid.bfa.runtime.persistence.cassandra;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
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
		RuntimeDatabaseFixture.clearData();
	}

	@Test
	public void allFailedFlowFieldsAreFetched() throws Exception {
		final Date startTime = new Date();
		RuntimeDatabaseFixture.insertFailedFlows(1, startTime, getParameters(), getCallStack(), getErrorDetails());

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.FAILED).setStartDate(startTime),
				new QueryPagingOptions());

		final List<ScriptRuntimeDTO> records = fetchResult.getResult();

		assertNull(fetchResult.getNextPageToken());
		assertEquals(1, records.size());

		ScriptRuntimeDTO rec = records.get(0);

		assertNotNull(rec.getRuntimeId());
		assertEquals(RuntimeDatabaseFixture.USER_NAME, rec.getUserName());
		assertEquals("Test Script 1", rec.getScriptName());
		assertEquals(getParameters(), rec.getParameters());
		assertEquals(addMinutes(startTime, 1), rec.getStartTime());
		assertEquals(addMinutes(startTime, 1), rec.getEndTime());
		assertEquals(getCallStack(), rec.getCallStack());
		assertEquals(getErrorDetails(), rec.getErrorDetails());
		assertEquals(RuntimeStatusType.FAILED, rec.getStatus());
	}

	@Test
	public void allRunningFlowFieldsAreFetched() throws Exception {
		final Date startTime = new Date();
		RuntimeDatabaseFixture.insertRunningFlows(1, startTime, getParameters(), getCallStack());

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.INPROGRESS).setStartDate(startTime),
				new QueryPagingOptions());

		final List<ScriptRuntimeDTO> records = fetchResult.getResult();

		assertNull(fetchResult.getNextPageToken());
		assertEquals(1, records.size());

		ScriptRuntimeDTO rec = records.get(0);

		assertNotNull(rec.getRuntimeId());
		assertEquals(RuntimeDatabaseFixture.USER_NAME, rec.getUserName());
		assertEquals("Test Script 1", rec.getScriptName());
		assertEquals(getParameters(), rec.getParameters());
		assertEquals(addMinutes(startTime, 1), rec.getStartTime());
		assertEquals(getCallStack(), rec.getCallStack());
		assertEquals(RuntimeStatusType.INPROGRESS, rec.getStatus());
	}

	@Test
	public void allCompletedFlowFieldsAreFetched() throws Exception {
		final Date startTime = new Date();
		RuntimeDatabaseFixture.insertCompletedFlows(1, startTime, getParameters(), getCallStack());

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.COMPLETED).setStartDate(startTime),
				new QueryPagingOptions());

		final List<ScriptRuntimeDTO> records = fetchResult.getResult();

		assertNull(fetchResult.getNextPageToken());
		assertEquals(1, records.size());

		ScriptRuntimeDTO rec = records.get(0);

		assertNotNull(rec.getRuntimeId());
		assertEquals(RuntimeDatabaseFixture.USER_NAME, rec.getUserName());
		assertEquals("Test Script 1", rec.getScriptName());
		assertEquals(getParameters(), rec.getParameters());
		assertEquals(addMinutes(startTime, 1), rec.getStartTime());
		assertEquals(addMinutes(startTime, 1), rec.getEndTime());
		assertEquals(getCallStack(), rec.getCallStack());
		assertEquals(RuntimeStatusType.COMPLETED, rec.getStatus());
	}

	@Test
	public void failedFlowsAreFetchedByDescendingStartTimeOrder() throws Exception {
		final Date startTime = new Date();
		RuntimeDatabaseFixture.insertFailedFlows(3, startTime);

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.FAILED).setStartDate(startTime),
				new QueryPagingOptions());

		assertResultIsSortedByDescendingStartTime(fetchResult, startTime);
	}

	@Test
	public void runningFlowsAreFetchedByDescendingStartTimeOrder() throws Exception {
		final Date startTime = new Date();
		RuntimeDatabaseFixture.insertRunningFlows(3, startTime);

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.INPROGRESS).setStartDate(startTime),
				new QueryPagingOptions());

		assertResultIsSortedByDescendingStartTime(fetchResult, startTime);
	}

	@Test
	public void completedFlowsAreFetchedByDescendingStartTimeOrder() throws Exception {
		final Date startTime = new Date();
		RuntimeDatabaseFixture.insertCompletedFlows(3, startTime);

		final QueryPage<ScriptRuntimeDTO> fetchResult = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(RuntimeStatusType.COMPLETED).setStartDate(startTime),
				new QueryPagingOptions());

		assertResultIsSortedByDescendingStartTime(fetchResult, startTime);
	}

	@Test
	public void fetchedFailedFlowsResultIsPaginated() throws Exception {
		final Date startTime = new Date();
		final int totalRecords = 100;
		final int pageSize = 80;

		RuntimeDatabaseFixture.insertFailedFlows(totalRecords, startTime);
		assertResultIsPaginated(totalRecords, pageSize, startTime, RuntimeStatusType.FAILED);
	}

	@Test
	public void fetchedRunningFlowsResultIsPaginated() throws Exception {
		final Date startTime = new Date();
		final int totalRecords = 100;
		final int pageSize = 80;

		RuntimeDatabaseFixture.insertRunningFlows(totalRecords, startTime);
		assertResultIsPaginated(totalRecords, pageSize, startTime, RuntimeStatusType.INPROGRESS);
	}

	@Test
	public void fetchedCompletedFlowsResultIsPaginated() throws Exception {
		final Date startTime = new Date();
		final int totalRecords = 100;
		final int pageSize = 80;

		RuntimeDatabaseFixture.insertCompletedFlows(totalRecords, startTime);
		assertResultIsPaginated(totalRecords, pageSize, startTime, RuntimeStatusType.COMPLETED);
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

	private void assertResultIsSortedByDescendingStartTime(QueryPage<ScriptRuntimeDTO> fetchResult, Date initTime) {
		final List<ScriptRuntimeDTO> records = fetchResult.getResult();

		assertNull(fetchResult.getNextPageToken());
		assertEquals(3, records.size());

		assertEquals(addMinutes(initTime, 3), records.get(0).getStartTime());
		assertEquals("Test Script 3", records.get(0).getScriptName());

		assertEquals(addMinutes(initTime, 2), records.get(1).getStartTime());
		assertEquals("Test Script 2", records.get(1).getScriptName());

		assertEquals(addMinutes(initTime, 1), records.get(2).getStartTime());
		assertEquals("Test Script 1", records.get(2).getScriptName());
	}

	private void assertResultIsPaginated(int totalRecords, int pageSize, Date startTime, RuntimeStatusType flowStatus)
			throws Exception {

		final QueryPagingOptions pagingOptions = new QueryPagingOptions().setResultsPerPage(pageSize);

		QueryPage<ScriptRuntimeDTO> firstPage = repository
				.fetch(new ScriptRuntimeCriteria().setStatus(flowStatus).setStartDate(startTime), pagingOptions);

		final String nextPageToken = firstPage.getNextPageToken();
		assertNotNull(nextPageToken);
		assertEquals(pageSize, firstPage.getResult().size());

		QueryPage<ScriptRuntimeDTO> secondPage = repository.fetch(
				new ScriptRuntimeCriteria().setStatus(flowStatus).setStartDate(startTime),
				pagingOptions.setPageToken(nextPageToken));

		assertNull(secondPage.getNextPageToken());
		assertEquals(totalRecords - pageSize, secondPage.getResult().size());
	}

}
