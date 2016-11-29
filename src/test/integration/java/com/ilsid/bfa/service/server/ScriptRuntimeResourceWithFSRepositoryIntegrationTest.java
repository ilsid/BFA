package com.ilsid.bfa.service.server;

import java.net.Inet4Address;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Test;

import com.ilsid.bfa.IntegrationTestConstants;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.ActionClassLoader;
import com.ilsid.bfa.common.DateHelper;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
import com.ilsid.bfa.runtime.monitor.MonitoringServer;
import com.ilsid.bfa.runtime.persistence.cassandra.RuntimeDatabaseFixture;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.RuntimeStatus;
import com.ilsid.bfa.service.dto.ScriptRuntimeParams;
import com.ilsid.bfa.service.dto.ScriptRuntimeQuery;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ScriptRuntimeResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String ACTION_NAME = "Write System Property";

	private static final String TEST_SYSTEM_PROP_NAME = "test.action.sys.property";

	private static final String ACTION_DIR = "action/default_group/write_x20_system_x20_property";

	private static final Set<Object> uniqueRuntimeIds = new HashSet<>();

	@After
	public void after() {
		System.getProperties().remove(TEST_SYSTEM_PROP_NAME);

		// The reload() procedure for the particular action releases all related resources (classes and jars). It is
		// necessary for the proper cleanup of the temporary testing directories
		ActionClassLoader.reload(ACTION_NAME);
	}

	@Test
	public void scriptIsRun() throws Exception {
		verifyScriptCanBeRun("ScriptToRead");
	}

	@Test
	public void scriptInNonDefaultGroupIsRun() throws Exception {
		// The script is run "Write System Property" action that sets "test.action.sys.property" system property
		assertNull(System.getProperty(TEST_SYSTEM_PROP_NAME));
		verifyScriptCanBeRun("Custom Group 02::Local Vars and Action Script");
		assertEquals("Test Action Value 333 55.77", System.getProperty(TEST_SYSTEM_PROP_NAME));
	}

	@Test
	public void scriptWithGeneratedEntityIsRun() throws Exception {
		// The script depends on Contract entity
		copyFileFromEntityDefaulGroupDirToRepository("Contract.class");
		verifyScriptCanBeRun("Single Entity Script");
	}

	@Test
	public void scriptWithSingleActionIsRun() throws Exception {
		copyDirectoryToRepository(TestConstants.CODE_REPOSITORY_DIR + "/" + ACTION_DIR, ACTION_DIR);

		// The script is run "Write System Property" action that sets "test.action.sys.property" system property
		assertNull(System.getProperty(TEST_SYSTEM_PROP_NAME));
		verifyScriptCanBeRun("Single Action Script");
		assertEquals("Test Action Value", System.getProperty(TEST_SYSTEM_PROP_NAME));
	}

	@Test
	public void scriptWithSingleParametrizedActionIsRun() throws Exception {
		verifyScriptWithParameterizedActionCanBeRun("Single Action With Params Script");
	}

	@Test
	public void scriptWithSingleParametrizedActionAndResultArrayIsRun() throws Exception {
		verifyScriptWithParameterizedActionCanBeRun("Single Action With Result Array Script");
	}

	@Test
	public void scriptWithSingleParametrizedSubflowIsRun() throws Exception {
		verifyScriptWithParameterizedActionCanBeRun("Single Subflow With Params Script");
	}

	@Test
	public void scriptWithInputParametersIsRun() throws Exception {
		verifyScriptWithInputParametersCanBeRun("Script with Params", 55, 9.99);
	}

	@Test
	public void scriptWithInputStringParametersIsRun() throws Exception {
		// Parameters can be represented as strings
		verifyScriptWithInputParametersCanBeRun("Script with Params", "55", "9.99");
	}

	@Test
	public void scriptWithInputJsonStringParameterIsRun() throws Exception {
		// First input param conforms Contract entity in the repository
		verifyScriptWithInputParametersCanBeRun("Custom Group 02::Script with Entity Param", "{\"Days\":\"55\"}",
				"9.99");
	}

	@Test
	public void scriptRuntimeRecordsAreFetched() throws Exception {
		try {
			final Date startDate = new Date();
			RuntimeDatabaseFixture.insertFailedFlows(5, startDate);
			// two records do not fit the query
			RuntimeDatabaseFixture.insertFailedFlows(2, DateHelper.addDays(startDate, 1));

			WebResource webResource = getWebResource(Paths.SCRIPT_RUNTIME_FETCH_SERVICE);

			ScriptRuntimeQuery query = new ScriptRuntimeQuery();
			ScriptRuntimeCriteria criteria = new ScriptRuntimeCriteria();
			criteria.setStartDate(startDate);
			criteria.setStatus(RuntimeStatusType.FAILED);
			query.setCriteria(criteria);

			ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, query);

			assertEquals(Status.OK.getStatusCode(), response.getStatus());

			@SuppressWarnings("unchecked")
			QueryPage<ScriptRuntimeDTO> result = response.getEntity(QueryPage.class);
			assertEquals(5, result.getResult().size());

		} finally {
			RuntimeDatabaseFixture.clearData();
		}

	}

	@Test
	@SuppressWarnings("unchecked")
	public void paginatedScriptRuntimeRecordsAreFetched() throws Exception {
		try {
			final Date startDate = new Date();
			RuntimeDatabaseFixture.insertFailedFlows(5, startDate);
			// two records do not fit the query
			RuntimeDatabaseFixture.insertFailedFlows(2, DateHelper.addDays(startDate, 1));

			WebResource webResource = getWebResource(Paths.SCRIPT_RUNTIME_FETCH_SERVICE);

			ScriptRuntimeQuery query = new ScriptRuntimeQuery();
			ScriptRuntimeCriteria criteria = new ScriptRuntimeCriteria();
			criteria.setStartDate(startDate);
			criteria.setStatus(RuntimeStatusType.FAILED);
			query.setCriteria(criteria);
			// 5 total records returned by query, but 3 records limit per page
			query.setResultsPerPage(3);

			final ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,
					query);

			assertEquals(Status.OK.getStatusCode(), response.getStatus());

			final QueryPage<ScriptRuntimeDTO> result = response.getEntity(QueryPage.class);
			assertEquals(3, result.getResult().size());
			assertNotNull(result.getNextPageToken());
			// Querying next page
			query.setPageToken(result.getNextPageToken());

			ClientResponse nextResponse = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,
					query);
			final QueryPage<ScriptRuntimeDTO> nextResult = nextResponse.getEntity(QueryPage.class);
			// Second page contains remaining 2 records
			assertEquals(2, nextResult.getResult().size());
			// No more pages
			assertNull(nextResult.getNextPageToken());

		} finally {
			RuntimeDatabaseFixture.clearData();
		}

	}

	@Test
	public void monitoringServerUrlIsObtained() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_RUNTIME_MONITORING_SERVER_URL_SERVICE);
		final ClientResponse response = webResource.get(ClientResponse.class);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertEquals("ws://" + Inet4Address.getLocalHost().getCanonicalHostName() + ":"
				+ IntegrationTestConstants.MONITORING_SERVER_PORT + MonitoringServer.CONTEXT_PATH
				+ MonitoringServer.MONITOR_END_POINT, response.getEntity(String.class));
	}

	private void verifyScriptWithParameterizedActionCanBeRun(String scriptName) throws Exception {
		copyDirectoryToRepository(TestConstants.CODE_REPOSITORY_DIR + "/" + ACTION_DIR, ACTION_DIR);

		// Script invokes "Write System Property" action that sets "test.action.sys.property" system property
		assertNull(System.getProperty(TEST_SYSTEM_PROP_NAME));
		verifyScriptCanBeRun(scriptName);
		// "Write System Property" accepts two parameters from input vars (passed by parent script). Their values are 3
		// and 5.4. The values are appended to the initial system property value "Test Action Value".
		assertEquals("Test Action Value 3 5.4", System.getProperty(TEST_SYSTEM_PROP_NAME));
	}

	private void verifyScriptWithInputParametersCanBeRun(String scriptName, Object... params) throws Exception {
		copyDirectoryToRepository(TestConstants.CODE_REPOSITORY_DIR + "/" + ACTION_DIR, ACTION_DIR);

		// The script invokes "Write System Property" action that sets "test.action.sys.property" system property
		assertNull(System.getProperty(TEST_SYSTEM_PROP_NAME));
		verifyScriptCanBeRun(scriptName, params);
		// "Write System Property" accepts two values from script's input parameters. Their values are 55 and 9.99. The
		// values are appended to the initial system property value "Test Action Value".
		assertEquals("Test Action Value 55 9.99", System.getProperty(TEST_SYSTEM_PROP_NAME));
	}

	private void verifyScriptCanBeRun(String scriptName) throws Exception {
		verifyScriptCanBeRun(scriptName, null);
	}

	private void verifyScriptCanBeRun(String scriptName, Object[] params) throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_RUN_SERVICE);
		ScriptRuntimeParams script = new ScriptRuntimeParams();
		script.setName(scriptName);

		if (params != null) {
			script.setInputParameters(params);
		}

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		RuntimeStatus status = response.getEntity(RuntimeStatus.class);

		final Object runtimeId = status.getRuntimeId();
		assertNotNull(runtimeId);
		assertTrue(uniqueRuntimeIds.add(runtimeId));

		assertEquals(RuntimeStatusType.COMPLETED, status.getStatusType());
	}

}