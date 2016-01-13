package com.ilsid.bfa.service.server;

import java.io.File;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.ScriptAdminParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ScriptAdminResourceWithFSRepositoryIntegrationTest extends FSCodeRepositoryIntegrationTest {

	private static final String GENERATED_SCRIPT_ROOT_PATH = "com/ilsid/bfa/generated/script/default_group";

	@Test
	public void validScriptIsCompiledAndItsSourceAndAllClassesAreSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Script 001",
				IOHelper.loadScript("duplicated-expression-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File scriptDir = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/script_x20_001");

		assertTrue(scriptDir.isDirectory());
		assertEquals(6, scriptDir.list().length);
		assertFilesExist(scriptDir.getPath(),
				new String[] { "Script_x20_001.class", "Script_x20_001.src", "Script_x20_001$$2.class",
						"Script_x20_001$$1.class", "Script_x20_001$$Var1_Mns_Var2.class",
						ClassNameUtil.METADATA_FILE_NAME });
	}

	@Test
	public void validScriptWithGeneratedEntityIsCompiledAndItsSourceAndAllClassesAreSavedInFileSystem()
			throws Exception {
		copyEntityFileToRepository("Contract.class");
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Entity Script",
				IOHelper.loadScript("single-entity-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File scriptDir = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/entity_x20_script");

		assertTrue(scriptDir.isDirectory());
		assertEquals(6, scriptDir.list().length);
		assertFilesExist(scriptDir.getPath(),
				new String[] { "Entity_x20_Script.class", "Entity_x20_Script.src", "Entity_x20_Script$$2.class",
						"Entity_x20_Script$$1.class", "Entity_x20_Script$$Var1_dt_Days_Mns_Var2.class",
						ClassNameUtil.METADATA_FILE_NAME });
	}

	@Test
	public void invalidScriptIsNotSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Script 002",
				IOHelper.loadScript("three-invalid-expressions-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class).startsWith("Compilation of the script [Script 002] failed"));
		assertFalse(new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/script002").exists());
	}

	@Test
	public void scriptWithoutNameIsNotSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams(null,
				IOHelper.loadScript("three-invalid-expressions-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class).startsWith("The name must be defined"));
	}

	@Test
	public void scriptWithoutBodyIsNotSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Script 002", null);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class).startsWith("The body must be defined"));
	}

	@Test
	public void validScriptAndItsSourceIsUpdatedInFileSystem() throws Exception {
		File scriptDir = new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/scripttoupdate");

		assertEquals(5, scriptDir.list().length);
		assertFilesExist(scriptDir.getPath(), new String[] { "ScriptToUpdate.class", "ScriptToUpdate.src",
				"ScriptToUpdate$$2.class", "ScriptToUpdate$$1.class", "ScriptToUpdate$$Var1_Mns_Var2.class" });

		WebResource webResource = getWebResource(Paths.SCRIPT_UPDATE_SERVICE);
		String updatedScriptBody = IOHelper.loadScript("duplicated-expression-script-upd.txt");
		ScriptAdminParams script = new ScriptAdminParams("ScriptToUpdate", updatedScriptBody);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		assertTrue(scriptDir.isDirectory());
		assertEquals(7, scriptDir.list().length);
		// Additional ScriptToUpdate$$3.class is persisted, as the expression "1" (that was duplicated) is replaced with
		// new expression "3".
		// ScriptToUpdate$$Var1_Mns_Var2.class is replaced with ScriptToUpdate$$Var1_Mns_Var3.class, as Var2 is replaced
		// with Var3 in "duplicated-expression-script-upd.txt" script
		assertFilesExist(scriptDir.getPath(),
				new String[] { "ScriptToUpdate.class", "ScriptToUpdate.src", "ScriptToUpdate$$3.class",
						"ScriptToUpdate$$1.class", "ScriptToUpdate$$Var1_Mns_Var3.class",
						ClassNameUtil.METADATA_FILE_NAME });

		String actualScriptBody = IOHelper.loadFileContents(scriptDir.getPath(), "ScriptToUpdate.src");
		assertEquals(updatedScriptBody, actualScriptBody);
	}

	@Test
	public void nonExistentScriptIsNotAllowedWhenTryingToUpdate() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_UPDATE_SERVICE);
		String updatedScriptBody = IOHelper.loadScript("duplicated-expression-script.txt");
		ScriptAdminParams script = new ScriptAdminParams("NonExistentScript", updatedScriptBody);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class)
				.startsWith("The script [NonExistentScript] does not exist in the repository"));
		assertFalse(new File(CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/nonexistentscript").exists());
	}

	@Test
	public void sourceCodeForExistingScriptIsLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_SOURCE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams();
		script.setName("ScriptToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String expectedSource = IOHelper.loadFileContents(
				CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/scripttoread", "ScriptToRead.src");

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

}
