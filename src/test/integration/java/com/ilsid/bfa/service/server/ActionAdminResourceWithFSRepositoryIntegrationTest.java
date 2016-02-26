package com.ilsid.bfa.service.server;

import java.io.File;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.ilsid.bfa.service.common.Paths;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ActionAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {
	
	private static final String ACTIONS_DIR = "action";
	
	private static final File ACTIONS_ROOT_DIR = new File(CODE_REPOSITORY_PATH, ACTIONS_DIR);
	
	//TODO: complete assertions
	@Test
	public void topLevelActionGroupCanBeCreated() throws Exception {
		WebResource webResource = getWebResource(Paths.ACTION_CREATE_GROUP_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, "Group 001");
		
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
	}

}
