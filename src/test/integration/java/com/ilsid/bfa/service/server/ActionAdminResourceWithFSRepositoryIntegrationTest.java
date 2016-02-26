package com.ilsid.bfa.service.server;

import java.io.File;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.service.common.Paths;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ActionAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String ACTIONS_DIR = "action";

	private static final File ACTIONS_ROOT_DIR = new File(CODE_REPOSITORY_PATH, ACTIONS_DIR);

	@Test
	public void topLevelActionGroupCanBeCreated() throws Exception {
		actionGroupCanBeCreated("Group 001", "Group 001", "group_x20_001");
	}

	@Test
	public void childActionGroupCanBeCreated() throws Exception {
		actionGroupCanBeCreated("Top Level Group 01::Child Group 02", "Child Group 02",
				"top_x20_level_x20_group_x20_01/child_x20_group_x20_02");
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
