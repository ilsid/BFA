package com.ilsid.bfa.service.server;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Test;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.ActionClassLoader;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.RuntimeStatus;
import com.ilsid.bfa.service.dto.ScriptRuntimeParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ScriptRuntimeResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String ACTION_NAME = "Write System Property";

	private static final String TEST_SYSTEM_PROP_NAME = "test.action.sys.property";

	private static final String ACTION_DIR = "action/default_group/Write_x20_System_x20_Property";
	
	private static final Set<Long> uniqueRuntimeIds = new HashSet<>();
	
	@After
	public void after() {
		System.getProperties().remove(TEST_SYSTEM_PROP_NAME);

		// The reload() procedure for the particular action releases all related resources (classes and jars). It is
		// necessary for the proper cleanup of the temporary testing directories
		ActionClassLoader.reload(ACTION_NAME);
	}

	@Test
	public void validScriptIsRun() throws Exception {
		verifyScriptCanBeRun("ScriptToRead");
	}

	@Test
	public void validScriptWithGeneratedEntityIsRun() throws Exception {
		// The script depends on Contract entity
		copyFileFromEntityDefaulGroupDirToRepository("Contract.class");
		verifyScriptCanBeRun("Single Entity Script");
	}

	@Test
	public void validScriptWithSingleActionIsRun() throws Exception {
		copyDirectoryToRepository(TestConstants.CODE_REPOSITORY_DIR + "/" + ACTION_DIR, ACTION_DIR);

		// The script is run "Write System Property" action that sets "test.action.sys.property" system property
		assertNull(System.getProperty(TEST_SYSTEM_PROP_NAME));
		verifyScriptCanBeRun("Single Action Script");
		assertEquals("Test Action Value", System.getProperty(TEST_SYSTEM_PROP_NAME));
	}

	@Test
	public void validScriptWithSingleParametrizedActionIsRun() throws Exception {
		copyDirectoryToRepository(TestConstants.CODE_REPOSITORY_DIR + "/" + ACTION_DIR, ACTION_DIR);

		// The script invokes "Write System Property" action that sets "test.action.sys.property" system property
		assertNull(System.getProperty(TEST_SYSTEM_PROP_NAME));
		verifyScriptCanBeRun("Single Action With Params Script");
		// "Write System Property" accepts two parameters. Their values are 3 and 5.4. The values are appended to the
		// initial system property value "Test Action Value".
		assertEquals("Test Action Value 3 5.4", System.getProperty(TEST_SYSTEM_PROP_NAME));
	}

	@Test
	public void validScriptWithSingleParametrizedSubflowIsRun() throws Exception {
		copyDirectoryToRepository(TestConstants.CODE_REPOSITORY_DIR + "/" + ACTION_DIR, ACTION_DIR);

		// The sub-flow script invokes "Write System Property" action that sets "test.action.sys.property" system
		// property
		assertNull(System.getProperty(TEST_SYSTEM_PROP_NAME));
		verifyScriptCanBeRun("Single Subflow With Params Script");
		// "Write System Property" accepts two parameters from input vars (passed by parent script). Their values are 3
		// and 5.4. The values are appended to the initial system property value "Test Action Value".
		assertEquals("Test Action Value 3 5.4", System.getProperty(TEST_SYSTEM_PROP_NAME));
	}

	private void verifyScriptCanBeRun(String scriptName) throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_RUN_SERVICE);
		ScriptRuntimeParams script = new ScriptRuntimeParams();
		script.setName(scriptName);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		RuntimeStatus status = response.getEntity(RuntimeStatus.class);
		
		final long runtimeId = status.getRuntimeId();
		assertTrue(runtimeId > 0);
		assertTrue(uniqueRuntimeIds.add(runtimeId));
		
		assertEquals(RuntimeStatusType.COMPLETED, status.getStatusType());
	}

}