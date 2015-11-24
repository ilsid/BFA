package com.ilsid.bfa.service.server;

import java.io.File;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.EntityAdminParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class EntityAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String GENERATED_ENTITY_ROOT_PATH = "com/ilsid/bfa/generated/entity/default_group";

	@Test
	public void validEntityIsCompiledAndItsSourceAndClassIsSavedInFileSystem() {
		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams("Entity001",
				"java.lang.Integer field1; java.lang.Double field2");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File entityDir = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_ROOT_PATH);

		assertTrue(entityDir.isDirectory());
		assertEquals(2, entityDir.list().length);
		assertFilesExist(entityDir.getPath(), new String[] { "Entity001.class", "Entity001.src" });
	}

}
