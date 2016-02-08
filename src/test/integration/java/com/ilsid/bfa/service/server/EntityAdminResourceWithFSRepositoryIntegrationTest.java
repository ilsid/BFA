package com.ilsid.bfa.service.server;

import java.io.File;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.EntityAdminParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class EntityAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String GENERATED_ENTITY_ROOT_PATH = ClassNameUtil.GENERATED_ENTITIES_ROOT_PACKAGE.replace('.',
			'/');

	private static final String GENERATED_ENTITY_DEFAULT_GROUP_PATH = ClassNameUtil.GENERATED_ENTITIES_DEFAULT_GROUP_PACKAGE
			.replace('.', '/');

	private static final String ENTITY_REPOSITORY_DEFAULT_GROUP_PATH = CODE_REPOSITORY_PATH + "/"
			+ GENERATED_ENTITY_DEFAULT_GROUP_PATH;

	private static final File ENTITY_REPOSITORY_DEFAULT_GROUP_DIR = new File(ENTITY_REPOSITORY_DEFAULT_GROUP_PATH);

	@Before
	public void initTest() throws Exception {
		FileUtils.cleanDirectory(ENTITY_REPOSITORY_DEFAULT_GROUP_DIR);
	}

	@Test
	public void validEntityIsCompiledAndItsSourceAndClassIsSavedInFileSystem() {
		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams("Entity001", "Number field1; Decimal field2");
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File entityDir = ENTITY_REPOSITORY_DEFAULT_GROUP_DIR;

		assertTrue(entityDir.isDirectory());
		assertEquals(2, entityDir.list().length);
		assertFilesExist(entityDir.getPath(), new String[] { "Entity001.class", "Entity001.src" });
	}

	@Test
	public void validEntityWithFieldOfGeneratedTypeIsCompiledAndItsSourceAndClassIsSavedInFileSystem()
			throws Exception {
		// Copy the generated class Contract to the code repository
		copyEntityFileToRepository("Contract.class");

		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		// The entity contains the generated class Contract
		EntityAdminParams entity = new EntityAdminParams("Entity003", "Number field1; Contract field2");
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File entityDir = ENTITY_REPOSITORY_DEFAULT_GROUP_DIR;

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

		File entityClassFile = new File(
				CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/Entity002.class");
		File entitySourceFile = new File(
				CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/Entity002.src");

		assertFalse(entityClassFile.exists());
		assertFalse(entitySourceFile.exists());
	}

	@Test
	public void validEntityAndItsSourceIsUpdatedInFileSystem() throws Exception {
		copyEntityFileToRepository("EntityToUpdate.class");
		copyEntityFileToRepository("EntityToUpdate.src");

		String initialEntityBody = IOHelper.loadFileContents(ENTITY_REPOSITORY_DEFAULT_GROUP_PATH,
				"EntityToUpdate.src");
		assertEquals("Number field1; Decimal field2", initialEntityBody);

		String newEntityBody = "Decimal field11; Number field22";
		EntityAdminParams entity = new EntityAdminParams("EntityToUpdate", newEntityBody);

		WebResource webResource = getWebResource(Paths.ENTITY_UPDATE_SERVICE);
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String updatedEntityBody = IOHelper.loadFileContents(ENTITY_REPOSITORY_DEFAULT_GROUP_PATH,
				"EntityToUpdate.src");
		assertEquals(newEntityBody, updatedEntityBody);
	}

	@Test
	public void nonExistentEntityIsNotAllowedWhenTryingToUpdate() throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_UPDATE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams("NonExistentEntity", "Number field1; Decimal field2");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The entity [NonExistentEntity] does not exist in the repository"));
		assertFalse(
				new File(CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/NonExistentEntity.class")
						.exists());
	}

	@Test
	public void sourceCodeForExistingEntityIsLoaded() throws Exception {
		copyEntityFileToRepository("EntityToRead.class");
		copyEntityFileToRepository("EntityToRead.src");

		WebResource webResource = getWebResource(Paths.ENTITY_GET_SOURCE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams();
		entity.setName("EntityToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertEquals("Number field22; Decimal field33; Number field44", response.getEntity(String.class));
	}

	@Test
	public void sourceCodeForNonExistingEntityIsNotLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_GET_SOURCE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams();
		entity.setName("NonExistingEntity");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertEquals("The entity [NonExistingEntity] does not exist in the repository",
				response.getEntity(String.class));
	}

	@Test
	public void topLevelEntityGroupCanBeCreated() throws Exception {
		entityGroupCanBeCreated("Some Top-Level Group", "Some Top-Level Group",
				CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_ROOT_PATH + "/some_x20_top_mns_level_x20_group");
	}

	@Test
	public void childScriptGroupCanBeCreated() throws Exception {
		entityGroupCanBeCreated("Custom Group 01::Some Child Group", "Some Child Group", CODE_REPOSITORY_PATH + "/"
				+ GENERATED_ENTITY_ROOT_PATH + "/custom_x20_group_x20_01/some_x20_child_x20_group");
	}

	private void entityGroupCanBeCreated(String groupName, String expectedTitle, String expectedPath) throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_GROUP_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, groupName);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		final File groupDir = new File(expectedPath);
		assertTrue(groupDir.isDirectory());

		Map<String, String> metaData = loadMetadata(new File(groupDir, ClassNameUtil.METADATA_FILE_NAME));
		assertEquals(2, metaData.keySet().size());
		assertEquals(groupName, metaData.get(Metadata.NAME));
		assertEquals(expectedTitle, metaData.get(Metadata.TITLE));

		FileUtils.forceDelete(groupDir);
	}

}
