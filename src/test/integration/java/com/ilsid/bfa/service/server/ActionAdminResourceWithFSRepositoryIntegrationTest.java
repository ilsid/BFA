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

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.service.common.Paths;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

public class ActionAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String ACTIONS_DIR = "action";

	private static final File ACTIONS_ROOT_DIR = new File(CODE_REPOSITORY_PATH, ACTIONS_DIR);

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
	public void childMetadataItemsAreNotLoadedIfGroupNameIsNotDefined() {
		WebResource webResource = getWebResource(Paths.ACTION_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void validActionCanBeCreatedInExistingGroup() throws Exception {
		// Archives the existing action and saves it with another name
		File actionZipFile = new File(ACTIONS_ROOT_DIR, "default_group/tmp_validAction.zip");
		IOHelper.zipDirectory(VALID_ACTION_DIR, actionZipFile);

		final String newActionPath = "top_x20_level_x20_group_x20_01/new_x20_action_x20_01";
		File newActionDir = new File(ACTIONS_ROOT_DIR, newActionPath);
		assertFalse(newActionDir.exists());

		WebResource webResource = getWebResource(Paths.ACTION_CREATE_SERVICE);

		try (InputStream is = new FileInputStream(actionZipFile)) {
			StreamDataBodyPart streamPart = new StreamDataBodyPart("file", is);
			@SuppressWarnings("resource")
			MultiPart entity = new FormDataMultiPart().field("name", "Top Level Group 01::New Action 01")
					.bodyPart(streamPart);
			ClientResponse response = webResource.type(MediaType.MULTIPART_FORM_DATA).post(ClientResponse.class,
					entity);

			assertEquals(Status.OK.getStatusCode(), response.getStatus());
		}

		assertTrue(newActionDir.isDirectory());
		IOHelper.assertEqualDirs(VALID_ACTION_DIR, newActionDir);

		File metaFile = new File(ACTIONS_ROOT_DIR, newActionPath + "/" + ClassNameUtil.METADATA_FILE_NAME);
		Map<String, String> metaData = IOHelper.toMap(metaFile);

		assertEquals(3, metaData.size());
		assertEquals(Metadata.ACTION_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Top Level Group 01::New Action 01", metaData.get(Metadata.NAME));
		assertEquals("New Action 01", metaData.get(Metadata.TITLE));
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

}
