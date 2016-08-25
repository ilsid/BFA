package com.ilsid.bfa.service.server;

import java.io.File;
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
import com.ilsid.bfa.service.dto.ScriptAdminParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ScriptAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String GENERATED_SCRIPT_ROOT_PATH = ClassNameUtil.GENERATED_SCRIPTS_ROOT_PACKAGE.replace('.',
			'/');

	private static final String GENERATED_SCRIPT_DEFAULT_GROUP_PATH = ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE
			.replace('.', '/');

	@Test
	public void validScriptIsCompiledAndItsSourceAndClassAreSavedInFileSystem() throws Exception {
		long version = getRepositoryVersion();

		scriptIsCompiledAndItsSourceAndClassAreSavedInFileSystem("Script 001",
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_DEFAULT_GROUP_PATH + "/script_x20_001");

		assertIncrementedVersion(version);
	}

	@Test
	public void validScriptInNonDefaultGroupIsCompiledAndItsSourceAndClassAreSavedInFileSystem() throws Exception {
		scriptIsCompiledAndItsSourceAndClassAreSavedInFileSystem("Custom Group 01::Script 001",
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/custom_x20_group_x20_01/script_x20_001");
	}

	@Test
	public void validScriptWithGeneratedEntityIsCompiledAndItsSourceAndAllClassesAreSavedInFileSystem()
			throws Exception {
		copyFileFromEntityDefaulGroupDirToRepository("Contract.class");
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Entity Script",
				IOHelper.loadScript("single-entity-script.txt"));

		File scriptDir = new File(
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_DEFAULT_GROUP_PATH + "/entity_x20_script");
		try {
			ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);
			assertEquals(Status.OK.getStatusCode(), response.getStatus());

			assertTrue(scriptDir.isDirectory());
			assertEquals(4, scriptDir.list().length);
			assertFilesExist(scriptDir.getPath(), new String[] { "Entity_x20_Script.class", "Entity_x20_Script.src",
					"Entity_x20_Script_generated.src", ClassNameUtil.METADATA_FILE_NAME });
		} finally {
			FileUtils.forceDelete(scriptDir);
		}
	}

	@Test
	public void inputParametersMetadataIsSavedForScriptWithInputVars() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Input Params Script",
				IOHelper.loadScript("input-vars-script.txt"));

		File scriptDir = new File(
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_DEFAULT_GROUP_PATH + "/input_x20_params_x20_script");
		try {
			ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);
			assertEquals(Status.OK.getStatusCode(), response.getStatus());

			assertTrue(scriptDir.isDirectory());

			File metaFile = new File(scriptDir, ClassNameUtil.METADATA_FILE_NAME);
			Map<String, String> params = IOHelper.toMap(loadMetadata(metaFile).get(Metadata.PARAMETERS));
			assertEquals(3, params.size());
			assertEquals("Number", params.get("Var1"));
			assertEquals("Decimal", params.get("Var2"));
			assertEquals("Number", params.get("Var3"));
		} finally {
			FileUtils.forceDelete(scriptDir);
		}
	}

	@Test
	public void invalidScriptIsNotSavedInFileSystem() throws Exception {
		long version = getRepositoryVersion();

		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Script 002",
				IOHelper.loadScript("three-invalid-expressions-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class).startsWith("Compilation of the script [Script 002] failed"));
		assertFalse(new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_DEFAULT_GROUP_PATH + "/script002").exists());
		assertSameVersion(version);
	}

	@Test
	public void scriptWithoutNameIsNotSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams(null,
				IOHelper.loadScript("three-invalid-expressions-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class).startsWith("The name must be defined"));
	}

	@Test
	public void scriptWithoutBodyIsNotSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Script 002", null);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class).startsWith("The body must be defined"));
	}

	@Test
	public void validScriptAndItsSourceIsUpdatedInFileSystem() throws Exception {
		long version = getRepositoryVersion();

		File scriptDir = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_DEFAULT_GROUP_PATH + "/scripttoupdate");

		assertEquals(4, scriptDir.list().length);
		assertFilesExist(scriptDir.getPath(), new String[] { "ScriptToUpdate.class", "ScriptToUpdate.src",
				"ScriptToUpdate_generated.src", ClassNameUtil.METADATA_FILE_NAME });

		WebResource webResource = getWebResource(Paths.SCRIPT_UPDATE_SERVICE);
		String updatedScriptBody = IOHelper.loadScript("duplicated-expression-script-upd.txt");
		ScriptAdminParams script = new ScriptAdminParams("ScriptToUpdate", updatedScriptBody, "Script to Update");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		assertTrue(scriptDir.isDirectory());
		assertEquals(4, scriptDir.list().length);
		assertFilesExist(scriptDir.getPath(), new String[] { "ScriptToUpdate.class", "ScriptToUpdate.src",
				"ScriptToUpdate_generated.src", ClassNameUtil.METADATA_FILE_NAME });

		String actualScriptBody = IOHelper.loadFileContents(scriptDir.getPath(), "ScriptToUpdate.src");
		assertEquals(updatedScriptBody, actualScriptBody);
		assertIncrementedVersion(version);
	}

	@Test
	public void nonExistentScriptIsNotAllowedWhenTryingToUpdate() throws Exception {
		long version = getRepositoryVersion();

		WebResource webResource = getWebResource(Paths.SCRIPT_UPDATE_SERVICE);
		String updatedScriptBody = IOHelper.loadScript("duplicated-expression-script.txt");
		ScriptAdminParams script = new ScriptAdminParams("NonExistentScript", updatedScriptBody);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The script [NonExistentScript] does not exist in the repository"));
		assertFalse(new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_DEFAULT_GROUP_PATH + "/nonexistentscript")
				.exists());
		assertSameVersion(version);
	}

	@Test
	public void sourceCodeForScriptIsLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_SOURCE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams();
		script.setName("ScriptToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String expectedSource = IOHelper.loadFileContents(
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_DEFAULT_GROUP_PATH + "/scripttoread", "ScriptToRead.src");

		assertEquals(expectedSource, response.getEntity(String.class));
	}

	@Test
	public void sourceCodeForScriptFromNonDefaultGroupIsLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_SOURCE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams();
		script.setName("Custom Group 01::Script 002");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String expectedSource = IOHelper.loadFileContents(
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/custom_x20_group_x20_01/script_x20_002",
				"Script_x20_002.src");

		assertEquals(expectedSource, response.getEntity(String.class));
	}

	@Test
	public void sourceCodeForScriptInNonDefaultGroupIsLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_SOURCE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams();
		script.setName("Custom Group 01::Script 002");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String expectedSource = IOHelper.loadFileContents(
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/custom_x20_group_x20_01/script_x20_002",
				"Script_x20_002.src");

		assertEquals(expectedSource, response.getEntity(String.class));
	}

	@Test
	public void sourceCodeForNonExistingScriptIsNotLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_SOURCE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams();
		script.setName("NonExistentScript");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The script [NonExistentScript] does not exist in the repository"));
	}

	@Test
	public void flowChartCodeForScriptIsLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_FLOW_CHART_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams();
		script.setName("ScriptToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String expectedResult = toUnixLF(IOHelper
				.loadScript("mermaid-adapter-expected-output/integration_tests/script-to-read-representation.txt"));

		assertEquals(expectedResult, response.getEntity(String.class));
	}

	@Test
	public void topLevelScriptGroupsAreLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, Metadata.ROOT_PARENT_NAME);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		@SuppressWarnings("unchecked")
		final List<Map<String, String>> metaDatas = response.getEntity(List.class);
		assertEquals(3, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Custom Group 01", metaData.get(Metadata.NAME));
		assertEquals("Custom Group 01", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.ROOT_PARENT_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(1);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Custom Group 02", metaData.get(Metadata.NAME));
		assertEquals("Custom Group 02", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.ROOT_PARENT_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(2);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.NAME));
		assertEquals(Metadata.DEFAULT_GROUP_TITLE, metaData.get(Metadata.TITLE));
		assertEquals(Metadata.ROOT_PARENT_NAME, metaData.get(Metadata.PARENT));
	}

	@Test
	public void subGroupAndScriptItemsWithDefinedMetadataAreLoaded() {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, Metadata.DEFAULT_GROUP_NAME);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		@SuppressWarnings("unchecked")
		final List<Map<String, String>> metaDatas = response.getEntity(List.class);

		assertEquals(6, metaDatas.size());

		// Note, if sub-group or script name does not contain explicit group it is treated as sub-group/script under
		// default_group

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("sub_group_001", metaData.get(Metadata.NAME));
		assertEquals("Sub-Group 001", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(1);
		assertEquals(5, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("default_group::Script with Params", metaData.get(Metadata.NAME));
		assertEquals("Script with Params", metaData.get(Metadata.TITLE));
		assertEquals("{\"Var1\":\"Number\",\"Var2\":\"Decimal\"}", metaData.get(Metadata.PARAMETERS));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(2);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("ScriptToRead", metaData.get(Metadata.NAME));
		assertEquals("ScriptToRead", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(3);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("ScriptToUpdate", metaData.get(Metadata.NAME));
		assertEquals("ScriptToUpdate", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(4);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Single Action Script", metaData.get(Metadata.NAME));
		assertEquals("Single Action Script", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));

		metaData = metaDatas.get(5);
		assertEquals(4, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Single Entity Script", metaData.get(Metadata.NAME));
		assertEquals("Single Entity Script", metaData.get(Metadata.TITLE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.PARENT));
	}

	@Test
	public void scriptInputParametersAreLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_INPUT_PARAMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, "Script with Params");

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		@SuppressWarnings("unchecked")
		Map<String, String> params = response.getEntity(Map.class);
		assertEquals(2, params.size());
		assertEquals("Number", params.get("Var1"));
		assertEquals("Decimal", params.get("Var2"));
	}

	@Test
	public void emptyScriptInputParametersAreLoadedIfNoInputVars() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_INPUT_PARAMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, "ScriptToRead");

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertEquals(0, response.getEntity(Map.class).size());
	}

	@Test
	public void childMetadataItemsAreNotLoadedIfGroupNameIsNotDefined() {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_ITEMS_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class);

		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	public void topLevelScriptGroupCanBeCreated() throws Exception {
		scriptGroupCanBeCreated("Some Top-Level Group", "Some Top-Level Group",
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/some_x20_top_mns_level_x20_group");
	}

	@Test
	public void childScriptGroupCanBeCreated() throws Exception {
		scriptGroupCanBeCreated("Custom Group 01::Some Child Group", "Some Child Group", CODE_REPOSITORY_PATH + "/"
				+ GENERATED_SCRIPT_ROOT_PATH + "/custom_x20_group_x20_01/some_x20_child_x20_group");
	}

	private void scriptIsCompiledAndItsSourceAndClassAreSavedInFileSystem(String scriptName, String expectedScriptPath)
			throws Exception {

		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams(scriptName,
				IOHelper.loadScript("duplicated-expression-script.txt"));

		File scriptDir = new File(expectedScriptPath);
		try {
			ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

			assertEquals(Status.OK.getStatusCode(), response.getStatus());

			assertTrue(scriptDir.isDirectory());
			assertEquals(4, scriptDir.list().length);
			assertFilesExist(scriptDir.getPath(), new String[] { "Script_x20_001.class", "Script_x20_001.src",
					"Script_x20_001_generated.src", ClassNameUtil.METADATA_FILE_NAME });
		} finally {
			FileUtils.forceDelete(scriptDir);
		}
	}

	private void scriptGroupCanBeCreated(String groupName, String expectedTitle, String expectedPath) throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_GROUP_SERVICE);
		ClientResponse response = webResource.post(ClientResponse.class, groupName);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		final File groupDir = new File(expectedPath);
		assertTrue(groupDir.isDirectory());

		Map<String, String> metaData = loadMetadata(new File(groupDir, ClassNameUtil.METADATA_FILE_NAME));
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(groupName, metaData.get(Metadata.NAME));
		assertEquals(expectedTitle, metaData.get(Metadata.TITLE));

		FileUtils.forceDelete(groupDir);
	}

}
