package com.ilsid.bfa.service.server;

import java.io.File;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.EntityAdminParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class EntityAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String GENERATED_ENTITY_ROOT_PATH = "com/ilsid/bfa/generated/entity/default_group";

	private static final File ENTITY_REPOSITORY_DIR = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_ROOT_PATH);

	@Before
	public void initTest() throws Exception {
		FileUtils.cleanDirectory(ENTITY_REPOSITORY_DIR);
	}

	@Test
	public void validEntityIsCompiledAndItsSourceAndClassIsSavedInFileSystem() {
		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams("Entity001", "Number field1; Decimal field2");
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File entityDir = ENTITY_REPOSITORY_DIR;

		assertTrue(entityDir.isDirectory());
		assertEquals(2, entityDir.list().length);
		assertFilesExist(entityDir.getPath(), new String[] { "Entity001.class", "Entity001.src" });
	}

	@Test
	public void validEntityWithFieldOfGeneratedTypeIsCompiledAndItsSourceAndClassIsSavedInFileSystem()
			throws Exception {
		// Copy the generated class Contract to the code repository
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/integration_tests/to_copy"),
				CODE_REPOSITORY_DIR);

		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		// The entity contains the generated class Contract
		EntityAdminParams entity = new EntityAdminParams("Entity003", "Number field1; Contract field2");
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File entityDir = ENTITY_REPOSITORY_DIR;

		assertTrue(entityDir.isDirectory());
		assertEquals(3, entityDir.list().length);
		assertFilesExist(entityDir.getPath(), new String[] { "Entity003.class", "Entity003.src" });
	}

	@Test
	public void invalidEntityIsIsNotSavedInFileSystem() {
		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams("Entity002", "Number field1; Decimal field2 field3");
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The entity [Entity002] contains invalid expression [Decimal field2 field3]"));

		File entityClassFile = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_ROOT_PATH + "/Entity002.class");
		File entitySourceFile = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_ROOT_PATH + "/Entity002.src");

		assertFalse(entityClassFile.exists());
		assertFalse(entitySourceFile.exists());
	}

}
