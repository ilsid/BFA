package com.ilsid.bfa.service.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.IntegrationTestConstants;
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
		copyFileFromEntityDefaulGroupDirToRepository(ClassNameUtil.METADATA_FILE_NAME);
	}

	@Test
	public void entityIsCompiledAndItsSourceAndClassIsSavedInFileSystem() throws Exception {
		long version = getRepositoryVersion();

		entityIsCompiledAndItsSourceAndClassIsSavedInFileSystem(
				new EntityAdminParams("Entity001", "{\"field1\":\"Number\", \"field2\":\"Decimal\"}"),
				ENTITY_REPOSITORY_DEFAULT_GROUP_DIR);

		assertIncrementedVersion(version);
	}

	@Test
	public void entityInNonDefaultGroupIsCompiledAndItsSourceAndClassIsSavedInFileSystem() throws Exception {
		File entityDir = new File(
				CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_ROOT_PATH + "/" + "custom_x20_group_x20_01");

		entityIsCompiledAndItsSourceAndClassIsSavedInFileSystem(
				new EntityAdminParams("Custom Group 01::Entity001", "{\"field1\":\"Number\", \"field2\":\"Decimal\"}"),
				entityDir);

		FileUtils.forceDelete(entityDir);
	}

	@Test
	public void entityWithFieldOfGeneratedTypeIsCompiledAndItsSourceAndClassIsSavedInFileSystem() throws Exception {
		// Copy the generated class Contract to the code repository
		copyFileFromEntityDefaulGroupDirToRepository("Contract.class");

		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		// The entity refers to the generated class Contract
		EntityAdminParams entity = new EntityAdminParams("Entity003",
				"{\"field1\":\"Number\", \"field2\":\"Contract\"}");
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File entityDir = ENTITY_REPOSITORY_DEFAULT_GROUP_DIR;

		assertTrue(entityDir.isDirectory());
		assertEquals(5, entityDir.list().length);
		assertFilesExist(entityDir.getPath(),
				new String[] { "Entity003.class", "Entity003.src", "Entity003_" + ClassNameUtil.METADATA_FILE_NAME });
	}

	@Test
	public void entityWithFieldOfGeneratedTypeInNonDefaultGroupIsCompiledAndItsSourceAndClassIsSavedInFileSystem()
			throws Exception {
		// Copy the generated class Subscriber residing in the group "Custom Group 01" (package
		// custom_x20_group_x20_01) to the code repository
		copyFileFromEntityDirToRepository("custom_x20_group_x20_01", "Subscriber.class");

		try {
			WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
			// The entity refers to the generated class [Custom Group 01::Subscriber]
			EntityAdminParams entity = new EntityAdminParams("Entity004",
					"{\"field1\":\"Number\", \"field2\":\"Custom Group 01::Subscriber\"}");
			ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

			assertEquals(Status.OK.getStatusCode(), response.getStatus());

			File entityDir = ENTITY_REPOSITORY_DEFAULT_GROUP_DIR;

			assertTrue(entityDir.isDirectory());
			assertEquals(4, entityDir.list().length);
			assertFilesExist(entityDir.getPath(), new String[] { "Entity004.class", "Entity004.src",
					"Entity004_" + ClassNameUtil.METADATA_FILE_NAME });
		} finally {
			deleteFileFromEntityRepository("custom_x20_group_x20_01/Subscriber.class");
		}
	}

	@Test
	public void invalidEntityIsIsNotSavedInFileSystem() throws Exception {
		long version = getRepositoryVersion();

		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams("Entity002", "Number field1; Decimal field2");
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The representation of entity [Entity002] has invalid format"));

		File entityClassFile = new File(
				CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/Entity002.class");
		File entitySourceFile = new File(
				CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/Entity002.src");

		assertFalse(entityClassFile.exists());
		assertFalse(entitySourceFile.exists());

		assertSameVersion(version);
	}

	@Test
	public void entityAndItsSourceIsUpdatedInFileSystem() throws Exception {
		long version = getRepositoryVersion();

		copyFileFromEntityDefaulGroupDirToRepository("EntityToUpdate.class");
		copyFileFromEntityDefaulGroupDirToRepository("EntityToUpdate.src");

		String initialEntityBody = IOHelper.loadFileContents(ENTITY_REPOSITORY_DEFAULT_GROUP_PATH,
				"EntityToUpdate.src");
		assertEquals("{\"field1\":\"Number\", \"field2\":\"Decimal\"}", initialEntityBody);

		String newEntityBody = "{\"field11\":\"Decimal\", \"field22\":\"Number\"}";
		EntityAdminParams entity = new EntityAdminParams("EntityToUpdate", newEntityBody);

		WebResource webResource = getWebResource(Paths.ENTITY_UPDATE_SERVICE);
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String updatedEntityBody = IOHelper.loadFileContents(ENTITY_REPOSITORY_DEFAULT_GROUP_PATH,
				"EntityToUpdate.src");
		assertEquals(newEntityBody, updatedEntityBody);

		assertIncrementedVersion(version);
	}

	@Test
	public void nonExistentEntityIsNotAllowedWhenTryingToUpdate() throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_UPDATE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams("NonExistentEntity",
				"{\"field1\":\"Number\", \"field2\":\"Decimal\"}");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The entity [NonExistentEntity] does not exist in the repository"));
		assertFalse(
				new File(CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/NonExistentEntity.class")
						.exists());
	}

	@Test
	public void entityIsDeletedFromFileSystem() throws Exception {
		copyFileFromEntityDefaulGroupDirToRepository("EntityToDelete.class");
		copyFileFromEntityDefaulGroupDirToRepository("EntityToDelete.src");
		copyFileFromEntityDefaulGroupDirToRepository("EntityToDelete_meta.data");

		File entityDir = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_DEFAULT_GROUP_PATH);

		assertFilesExist(entityDir.getPath(),
				new String[] { "EntityToDelete.class", "EntityToDelete.src", "EntityToDelete_meta.data" });

		WebResource webResource = getWebResource(Paths.ENTITY_DELETE_SERVICE);
		ClientResponse response = webResource.type(MediaType.TEXT_PLAIN).post(ClientResponse.class, "EntityToDelete");

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		assertFalse(new File(entityDir, "EntityToDelete.class").exists());
		assertFalse(new File(entityDir, "EntityToDelete.src").exists());
		assertFalse(new File(entityDir, "EntityToDelete_meta.data").exists());
	}

	@Test
	public void nonExistentEntityIsNotAllowedWhenTryingToDelete() throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_DELETE_SERVICE);
		ClientResponse response = webResource.type(MediaType.TEXT_PLAIN).post(ClientResponse.class,
				"NonExistentEntity");

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The entity [NonExistentEntity] does not exist in the repository"));
	}

	@Test
	public void sourceCodeForEntityIsLoaded() throws Exception {
		copyFileFromEntityDefaulGroupDirToRepository("EntityToRead.class");
		copyFileFromEntityDefaulGroupDirToRepository("EntityToRead.src");

		WebResource webResource = getWebResource(Paths.ENTITY_GET_SOURCE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams();
		entity.setName("EntityToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertEquals("{\"field22\":\"Number\", \"field33\":\"Decimal\", \"field44\":\"Number\"}",
				response.getEntity(String.class));
	}

	@Test
	public void sourceCodeForEntityInNonDefaultGroupIsLoaded() throws Exception {
		// Copy the generated class Subscriber and its source residing in the group "Custom Group 01" (package
		// custom_x20_group_x20_01) to the code repository
		copyFileFromEntityDirToRepository("custom_x20_group_x20_01", "Subscriber.class");
		copyFileFromEntityDirToRepository("custom_x20_group_x20_01", "Subscriber.src");
		try {
			WebResource webResource = getWebResource(Paths.ENTITY_GET_SOURCE_SERVICE);
			EntityAdminParams entity = new EntityAdminParams();
			entity.setName("Custom Group 01::Subscriber");
			ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);
			assertEquals(Status.OK.getStatusCode(), response.getStatus());
			assertEquals("{\"PrepaidAmount\":\"Decimal\", \"PrepaidReserved\":\"Decimal\", \"PrepaidDays\":\"Number\"}",
					response.getEntity(String.class));
		} finally {
			deleteFileFromEntityRepository("custom_x20_group_x20_01/Subscriber.class");
			deleteFileFromEntityRepository("custom_x20_group_x20_01/Subscriber.src");
		}
	}

	@Test
	public void sourceCodeForNonExistingEntityIsNotLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_GET_SOURCE_SERVICE);
		EntityAdminParams entity = new EntityAdminParams();
		entity.setName("NonExistingEntity");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertEquals("The entity [NonExistingEntity] does not exist in the repository",
				response.getEntity(String.class));
	}

	@Test
	public void topLevelEntityGroupCanBeCreated() throws Exception {
		entityGroupCanBeCreated("Some Top-Level Group", "Some Top-Level Group",
				CODE_REPOSITORY_PATH + "/" + GENERATED_ENTITY_ROOT_PATH + "/some_x20_top_mns_level_x20_group");
	}

	@Test
	public void childEntityGroupCanBeCreated() throws Exception {
		entityGroupCanBeCreated("Custom Group 01::Some Child Group", "Some Child Group", CODE_REPOSITORY_PATH + "/"
				+ GENERATED_ENTITY_ROOT_PATH + "/custom_x20_group_x20_01/some_x20_child_x20_group");
	}

	@Test
	public void topLevelEntityGroupsAreLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, Metadata.ROOT_PARENT_NAME);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		@SuppressWarnings("unchecked")
		final List<Map<String, String>> metaDatas = response.getEntity(List.class);
		assertEquals(2, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Custom Group 01", metaData.get(Metadata.NAME));
		assertEquals("Custom Group 01", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.ROOT_PARENT_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(1);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.NAME));
		assertEquals(Metadata.DEFAULT_GROUP_TITLE, metaData.get(Metadata.TITLE));
		assertEquals(Metadata.ROOT_PARENT_NAME, metaData.get(Metadata.PARENT));
	}

	@Test
	public void subGroupAndEntityItemsWithDefinedMetadataAreLoaded() throws Exception {
		copyEntityDirectoryToRepository(Metadata.DEFAULT_GROUP_NAME);

		WebResource webResource = getWebResource(Paths.ENTITY_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, Metadata.DEFAULT_GROUP_NAME);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		@SuppressWarnings("unchecked")
		final List<Map<String, String>> metaDatas = response.getEntity(List.class);
		assertEquals(4, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("sub_group_01", metaData.get(Metadata.NAME));
		assertEquals("sub_group_01", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(1);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("default_group::EntityToDelete", metaData.get(Metadata.NAME));
		assertEquals("EntityToDelete", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(2);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("EntityToRead", metaData.get(Metadata.NAME));
		assertEquals("EntityToRead", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(3);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("EntityToUpdate", metaData.get(Metadata.NAME));
		assertEquals("EntityToUpdate", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));
	}

	@Test
	public void enitiesLibraryIsLoaded() throws Exception {
		copyEntityDirectoryToRepository(Metadata.DEFAULT_GROUP_NAME);
		copyEntityDirectoryToRepository("custom_x20_group_x20_01");

		WebResource webResource = getWebResource(Paths.ENTITY_GET_LIBRARY_SERVICE);
		ClientResponse response = webResource.get(ClientResponse.class);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		InputStream is = response.getEntityInputStream();
		String jarName = extractEntitiesJarName(response);
		assertEquals("bfa-entities.jar", jarName);
		final File jarFile = new File(IntegrationTestConstants.CODE_REPOSITORY_DIR, jarName);
		try (OutputStream os = new FileOutputStream(jarFile)) {
			IOUtils.copyLarge(is, os);
		}

		verifyEntitiesJar(jarFile);
	}

	private String extractEntitiesJarName(ClientResponse response) {
		String header = response.getHeaders().get("Content-Disposition").get(0);
		final String fileNameExpr = "filename=";
		int fileNameIdx = header.lastIndexOf(fileNameExpr);
		String jarName = header.substring(fileNameIdx + fileNameExpr.length());

		return jarName;
	}

	private void verifyEntitiesJar(File jarFile) throws Exception {
		final File jarDir = new File(IntegrationTestConstants.CODE_REPOSITORY_DIR, "__tmp_jar");
		List<File> jarFiles = IOHelper.unzip(jarFile, jarDir);

		assertEquals(6, jarFiles.size());

		assertTrue(jarFiles.contains(new File(jarDir, "META-INF/MANIFEST.MF")));
		assertTrue(jarFiles
				.contains(new File(jarDir, GENERATED_ENTITY_ROOT_PATH + "/custom_x20_group_x20_01/Subscriber.class")));
		assertTrue(jarFiles.contains(new File(jarDir, GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/Contract.class")));
		assertTrue(jarFiles.contains(new File(jarDir, GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/EntityToRead.class")));
		assertTrue(jarFiles.contains(new File(jarDir, GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/EntityToUpdate.class")));
		assertTrue(jarFiles.contains(new File(jarDir, GENERATED_ENTITY_DEFAULT_GROUP_PATH + "/EntityToDelete.class")));
	}

	private void entityGroupCanBeCreated(String groupName, String expectedTitle, String expectedPath) throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_GROUP_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, groupName);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		final File groupDir = new File(expectedPath);
		assertTrue(groupDir.isDirectory());

		Map<String, String> metaData = loadMetadata(new File(groupDir, ClassNameUtil.METADATA_FILE_NAME));
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(groupName, metaData.get(Metadata.NAME));
		assertEquals(expectedTitle, metaData.get(Metadata.TITLE));

		FileUtils.forceDelete(groupDir);
	}

	private void entityIsCompiledAndItsSourceAndClassIsSavedInFileSystem(EntityAdminParams entity, File entityDir)
			throws Exception {
		WebResource webResource = getWebResource(Paths.ENTITY_CREATE_SERVICE);
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, entity);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		assertTrue(entityDir.isDirectory());
		assertEquals(4, entityDir.list().length);
		assertFilesExist(entityDir.getPath(), new String[] { "Entity001.class", "Entity001.src",
				"Entity001_" + ClassNameUtil.METADATA_FILE_NAME, ClassNameUtil.METADATA_FILE_NAME });
	}

}
