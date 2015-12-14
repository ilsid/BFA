package com.ilsid.bfa.service.server;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.ActionClassLoader;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.RuntimeStatus;
import com.ilsid.bfa.service.dto.RuntimeStatusType;
import com.ilsid.bfa.service.dto.ScriptRuntimeParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ScriptRuntimeResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String ACTION_DIR = "action/default_group/Write_x20_System_x20_Property";

	@Test
	public void validScriptIsRun() throws Exception {
		verifyScriptCanBeRun("ScriptToRead");
	}

	@Test
	public void validScriptWithGeneratedEntityIsRun() throws Exception {
		// The script depends on Contract entity
		copyEntityFileToRepository("Contract.class");
		verifyScriptCanBeRun("Single Entity Script");
	}

	@Test
	public void validScriptWithSingleActionIsRun() throws Exception {
		copyDirectoryToRepository(TestConstants.CODE_REPOSITORY_DIR + "/" + ACTION_DIR, ACTION_DIR);

		// The script is run "Write System Property" action that sets "test.action.sys.property" system property
		final String sysPropName = "test.action.sys.property";
		assertNull(System.getProperty(sysPropName));
		verifyScriptCanBeRun("Single Action Script");
		assertEquals("Test Action Value", System.getProperty(sysPropName));

		// The reload() procedure for the particular action releases all related resources (classes and jars). It is
		// necessary for the proper cleanup of the temporary testing directories
		ActionClassLoader.reload("Write System Property");
	}

	private void verifyScriptCanBeRun(String scriptName) throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_RUN_SERVICE);
		ScriptRuntimeParams script = new ScriptRuntimeParams();
		script.setName(scriptName);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		RuntimeStatus status = response.getEntity(RuntimeStatus.class);
		assertTrue(status.getRuntimeId() > 0);
		assertEquals(RuntimeStatusType.COMPLETED, status.getStatusType());
	}

}