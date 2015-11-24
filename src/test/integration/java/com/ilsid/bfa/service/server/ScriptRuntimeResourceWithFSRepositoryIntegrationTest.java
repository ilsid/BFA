package com.ilsid.bfa.service.server;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.RuntimeStatus;
import com.ilsid.bfa.service.dto.RuntimeStatusType;
import com.ilsid.bfa.service.dto.ScriptRuntimeParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ScriptRuntimeResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	@Test
	public void validScriptIsRun() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_RUN_SERVICE);
		ScriptRuntimeParams script = new ScriptRuntimeParams();
		script.setName("ScriptToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		RuntimeStatus status = response.getEntity(RuntimeStatus.class);
		assertTrue(status.getRuntimeId() > 0);
		assertEquals(RuntimeStatusType.COMPLETED, status.getStatusType());
	}

}