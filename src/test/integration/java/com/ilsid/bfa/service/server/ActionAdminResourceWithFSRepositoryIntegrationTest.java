package com.ilsid.bfa.service.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ilsid.bfa.IntegrationTestConstants;
import com.ilsid.bfa.action.persistence.ActionClassLoader;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.manager.ActionManager.ActionDetails;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.ActionAdminParams;
import com.ilsid.bfa.service.dto.OperationStatus;
import com.ilsid.bfa.service.dto.OperationStatus.Failure;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

public class ActionAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String EXISTING_ACTION_IMPL_CLASS_NAME = "com.some.action.impl.WriteSystemProperty";

	private static final String NEW_ACTION_NAME = "Top Level Group 01::New Action 01";

	private static final String EXISTING_ACTION_NAME = "Write System Property";

	private static final String ACTIONS_DIR = "action";

	private static final File ACTIONS_ROOT_DIR = new File(IntegrationTestConstants.CODE_REPOSITORY_DIR, ACTIONS_DIR);

	private static final File VALID_ACTION_DIR = new File(ACTIONS_ROOT_DIR,
			"default_group/write_x20_system_x20_property");

	@Test
	public void topLevelActionGroupCanBeCreated() throws Exception {
		actionGroupCanBeCreated("Group 001", "Group 001", "group_x20_001");
	}

	@Test
	public void childActionGroupCanBeCreated() throws Exception {
		actionGroupCanBeCreated("Top Level Group 01::Child Group 007", "Child Group 007",
				"top_x20_level_x20_group_x20_01/child_x20_group_x20_007");
	}

	@Test
	public void topLevelActionGroupsAreLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.ACTION_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, Metadata.ROOT_PARENT_NAME);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		@SuppressWarnings("unchecked")
		final List<Map<String, String>> metaDatas = response.getEntity(List.class);
		assertEquals(2, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.NAME));
		assertEquals(Metadata.DEFAULT_GROUP_TITLE, metaData.get(Metadata.TITLE));
		assertEquals(Metadata.ROOT_PARENT_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(1);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Top Level Group 01", metaData.get(Metadata.NAME));
		assertEquals("Top Level Group 01", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.ROOT_PARENT_NAME, metaData.get(Metadata.PARENT));
	}

	@Test
	public void subGroupsWithDefinedMetadataAreLoaded() {
		WebResource webResource = getWebResource(Paths.ACTION_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, "Top Level Group 01");

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		@SuppressWarnings("unchecked")
		final List<Map<String, String>> metaDatas = response.getEntity(List.class);

		assertEquals(2, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Top Level Group 01::Child Group 01", metaData.get(Metadata.NAME));
		assertEquals("Child Group 01", metaData.get(Metadata.TITLE));
		assertEquals("Top Level Group 01", metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(1);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Top Level Group 01::Child Group 02", metaData.get(Metadata.NAME));
		assertEquals("Child Group 02", metaData.get(Metadata.TITLE));
		assertEquals("Top Level Group 01", metaData.get(Metadata.PARENT));
	}

	@Test
	public void subGroupsAndChildActionsWithDefinedMetadataAreLoaded() {
		WebResource webResource = getWebResource(Paths.ACTION_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		@SuppressWarnings("unchecked")
		final List<Map<String, String>> metaDatas = response.getEntity(List.class);

		assertEquals(2, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Child Group 01", metaData.get(Metadata.NAME));
		assertEquals("Child Group 01", metaData.get(Metadata.TITLE));
		assertEquals(ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(1);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.ACTION_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Write System Property", metaData.get(Metadata.NAME));
		assertEquals("Write System Property", metaData.get(Metadata.TITLE));
		assertEquals(ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE, metaData.get(Metadata.PARENT));
	}

	@Test
	public void childMetadataItemsAreNotLoadedIfGroupNameIsNotDefined() {
		WebResource webResource = getWebResource(Paths.ACTION_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void actionIsCreatedInExistingGroup() throws Exception {
		long version = getRepositoryVersion();

		ClientResponse response = tryCreateAction(Paths.ACTION_CREATE_SERVICE, NEW_ACTION_NAME, true);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertNotNull(response.getEntity(OperationStatus.Success.class));
		assertIncrementedVersion(version);
	}

	@Test
	public void actionIsCreatedQuietlyInExistingGroup() throws Exception {
		ClientResponse response = tryCreateAction(Paths.ACTION_CREATE_QUIETLY_SERVICE, NEW_ACTION_NAME, true);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertNotNull(response.getEntity(OperationStatus.Success.class));
	}

	@Test
	public void actionIsNotCreatedIfActionNameIsNotDefined() throws Exception {
		long version = getRepositoryVersion();

		ClientResponse response = tryCreateAction(Paths.ACTION_CREATE_SERVICE, "", false);
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertSameVersion(version);
	}

	@Test
	public void actionIsNotCreatedQuietlyIfActionNameIsNotDefined() throws Exception {
		ClientResponse response = tryCreateAction(Paths.ACTION_CREATE_QUIETLY_SERVICE, "", false);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		final Failure respEntity = response.getEntity(OperationStatus.Failure.class);
		assertTrue(respEntity.getValue().startsWith("Error:"));
	}

	@Test
	public void actionInfoIsLoaded() {
		WebResource webResource = getWebResource(Paths.ACTION_GET_INFO_SERVICE);
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,
				new ActionAdminParams("Write System Property"));

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		ActionDetails info = response.getEntity(ActionDetails.class);
		assertEquals(EXISTING_ACTION_IMPL_CLASS_NAME, info.getImplementationClassName());
		final List<String> dependencies = info.getDependencies();
		assertEquals(2, dependencies.size());
		assertTrue(dependencies.contains("commons-collections-3.2.1.jar"));
		assertTrue(dependencies.contains("mail-1.4.1.jar"));
	}

	@Test
	public void actionInfoIsNotLoadedIfActionNameIsNotDefined() {
		WebResource webResource = getWebResource(Paths.ACTION_GET_INFO_SERVICE);
		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class,
				new ActionAdminParams());

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void existingActionCanBeUpdatedInFileSystemAndActionClassesAreReloaded() throws Exception {
		updateAndVerifyAction(Paths.ACTION_UPDATE_SERVICE);
	}

	@Test
	public void existingActionCanBeUpdatedQuietlyInFileSystemAndActionClassesAreReloaded() throws Exception {
		updateAndVerifyAction(Paths.ACTION_UPDATE_QUIETLY_SERVICE);
	}

	@Test
	public void existingActionCanBeDeletedFromFileSystem() throws Exception {
		final String actionToDeleteSubDir = "write_x20_system_x20_property_x20_2";
		copyActionFromDefaultGroupToRepository(actionToDeleteSubDir);

		final File actionToDeleteDir = new File(ACTION_REPOSITORY_DEFAULT_GROUP_DIR_PATH, actionToDeleteSubDir);
		assertTrue(actionToDeleteDir.isDirectory());

		final String actionNameToDelete = "Write System Property 2";

		// make sure the action implementation can be loaded
		ActionClassLoader.getLoader(actionNameToDelete).loadClass(EXISTING_ACTION_IMPL_CLASS_NAME);

		WebResource webResource = getWebResource(Paths.ACTION_DELETE_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, actionNameToDelete);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		// make sure the action implementation is no longer available
		try {
			ActionClassLoader.getLoader(actionNameToDelete).loadClass(EXISTING_ACTION_IMPL_CLASS_NAME);
			fail(ClassNotFoundException.class.getName() + " was expected to raise");
		} catch (ClassNotFoundException e) {
		}

		assertFalse(actionToDeleteDir.exists());
	}

	@Test
	public void nonExistentActionIsNotAllowedWhenTryingToDelete() throws Exception {
		WebResource webResource = getWebResource(Paths.ACTION_DELETE_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, "NonExistentAction");

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The action [NonExistentAction] does not exist in the repository"));
	}

	private void actionGroupCanBeCreated(String groupName, String expectedTitle, String expectedPath) throws Exception {
		WebResource webResource = getWebResource(Paths.ACTION_CREATE_GROUP_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, groupName);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		final File groupDir = new File(ACTIONS_ROOT_DIR, expectedPath);
		assertTrue(groupDir.isDirectory());

		Map<String, String> metaData = loadMetadata(new File(groupDir, ClassNameUtil.METADATA_FILE_NAME));
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(groupName, metaData.get(Metadata.NAME));
		assertEquals(expectedTitle, metaData.get(Metadata.TITLE));

		FileUtils.forceDelete(groupDir);
	}

	private ClientResponse tryCreateAction(String serviceName, String actionName, boolean verifyNewAction)
			throws Exception {
		// Archives the existing action and saves it with another name
		File actionZipFile = new File(ACTIONS_ROOT_DIR, "default_group/tmp_validAction.zip");
		IOHelper.zipDirectory(VALID_ACTION_DIR, actionZipFile);

		final String newActionPath = "top_x20_level_x20_group_x20_01/new_x20_action_x20_01";
		File newActionDir = new File(ACTIONS_ROOT_DIR, newActionPath);
		assertFalse(newActionDir.exists());

		WebResource webResource = getWebResource(serviceName);
		ClientResponse response;
		try {
			try (InputStream is = new FileInputStream(actionZipFile)) {
				StreamDataBodyPart streamPart = new StreamDataBodyPart("file", is);
				@SuppressWarnings("resource")
				MultiPart entity = new FormDataMultiPart().field("name", actionName).bodyPart(streamPart);
				response = webResource.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class, entity);
			}

			if (verifyNewAction) {
				assertTrue(newActionDir.isDirectory());
				IOHelper.assertEqualDirs(VALID_ACTION_DIR, newActionDir);

				File metaFile = new File(ACTIONS_ROOT_DIR, newActionPath + "/" + ClassNameUtil.METADATA_FILE_NAME);
				Map<String, String> metaData = IOHelper.toMap(metaFile);

				assertEquals(3, metaData.size());
				assertEquals(Metadata.ACTION_TYPE, metaData.get(Metadata.TYPE));
				assertEquals(actionName, metaData.get(Metadata.NAME));
				assertEquals("New Action 01", metaData.get(Metadata.TITLE));
			}
		} finally {
			if (newActionDir.exists()) {
				FileUtils.forceDelete(newActionDir);
			}
		}

		return response;
	}

	private void updateAndVerifyAction(String serviceName) throws Exception {
		try {
			long version = getRepositoryVersion();

			Class<?> oldActionImplClazz = ActionClassLoader.getLoader(EXISTING_ACTION_NAME)
					.loadClass(EXISTING_ACTION_IMPL_CLASS_NAME);
			// One of classes in dependency mail-1.4.1.jar
			final String dependencyClassName = "javax.mail.util.ByteArrayDataSource";
			Class<?> oldActionDependencyClazz = ActionClassLoader.getLoader(EXISTING_ACTION_NAME)
					.loadClass(dependencyClassName);

			// Archives the existing action and saves it again
			File actionZipFile = new File(ACTIONS_ROOT_DIR, "default_group/tmp_validAction.zip");
			IOHelper.zipDirectory(VALID_ACTION_DIR, actionZipFile);
			final File expectedActionDir = new File(ACTIONS_ROOT_DIR, "default_group/__tmp_unpacked_action");
			IOHelper.unzip(actionZipFile, expectedActionDir);

			WebResource webResource = getWebResource(serviceName);
			ClientResponse response;
			try (InputStream is = new FileInputStream(actionZipFile)) {
				StreamDataBodyPart streamPart = new StreamDataBodyPart("file", is);
				@SuppressWarnings("resource")
				MultiPart entity = new FormDataMultiPart().field("name", EXISTING_ACTION_NAME).bodyPart(streamPart);
				response = webResource.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class, entity);

				assertEquals(Status.OK.getStatusCode(), response.getStatus());
			}

			assertTrue(VALID_ACTION_DIR.isDirectory());
			IOHelper.assertEqualDirs(VALID_ACTION_DIR, expectedActionDir);

			Class<?> updatedActionImplClazz = ActionClassLoader.getLoader(EXISTING_ACTION_NAME)
					.loadClass(EXISTING_ACTION_IMPL_CLASS_NAME);
			Class<?> updatedActionDependencyClazz = ActionClassLoader.getLoader(EXISTING_ACTION_NAME)
					.loadClass(dependencyClassName);

			// Action implementation class and its dependencies are reloaded
			assertFalse(oldActionImplClazz == updatedActionImplClazz);
			assertFalse(oldActionDependencyClazz == updatedActionDependencyClazz);

			assertIncrementedVersion(version);
		} finally {
			// Resources occupied by the action loader are released
			ActionClassLoader.releaseResources(EXISTING_ACTION_NAME);
		}
	}
}
