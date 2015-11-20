package com.ilsid.bfa.service.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.LoggingConfigurator;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.filesystem.FSCodeRepository;
import com.ilsid.bfa.service.common.Paths;
import com.ilsid.bfa.service.dto.RuntimeStatus;
import com.ilsid.bfa.service.dto.RuntimeStatusType;
import com.ilsid.bfa.service.dto.ScriptAdminParams;
import com.ilsid.bfa.service.dto.ScriptRuntimeParams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class ScriptResourceWithFSRepositoryIntegrationTest extends RESTServiceIntegrationTestCase {

	private static final String LOGGING_CONFIG_FILE = TestConstants.TEST_RESOURCES_DIR + "/log4j.xml";

	private static final String GENERATED_SCRIPT_ROOT_PATH = "com/ilsid/bfa/generated/script/default_group";

	private static final File CODE_REPOSITORY_DIR = new File(FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH);

	@BeforeClass
	public static void setUp() throws Exception {
		FileUtils.forceMkdir(CODE_REPOSITORY_DIR);
		LoggingConfigurator.configureLog4j(LOGGING_CONFIG_FILE);

		// Creating the "pre-condition" scripts required for some tests
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/integration_tests/code_repository"),
				CODE_REPOSITORY_DIR);

		startWebServer(new FSRepositoryApplicationConfig());
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopWebServer();
		FileUtils.forceDelete(CODE_REPOSITORY_DIR);
	}

	@Test
	public void validScriptIsCompiledAndItsSourceAndAllClassesAreSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Script 001",
				IOHelper.loadScript("duplicated-expression-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		File scriptDir = new File(
				FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/script_x20_001");

		assertTrue(scriptDir.isDirectory());
		assertEquals(5, scriptDir.list().length);
		assertFilesExist(scriptDir.getPath(), new String[] { "Script_x20_001.class", "Script_x20_001.src", "Script_x20_001$$2.class",
				"Script_x20_001$$1.class", "Script_x20_001$$Var1_Mns_Var2.class" });
	}

	@Test
	public void invalidScriptIsNotSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_CREATE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams("Script 002",
				IOHelper.loadScript("two-invalid-expressions-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertTrue(response.getEntity(String.class).startsWith("Compilation of the script [Script 002] failed"));
		assertFalse(new File(
				FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/script002")
						.exists());
	}

	@Test
	public void validScriptAndItsSourceIsUpdatedInFileSystem() throws Exception {
		File scriptDir = new File(FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH
				+ "/scripttoupdate");

		assertEquals(5, scriptDir.list().length);
		assertFilesExist(scriptDir.getPath(), new String[] { "ScriptToUpdate.class", "ScriptToUpdate.src",
				"ScriptToUpdate$$2.class", "ScriptToUpdate$$1.class", "ScriptToUpdate$$Var1_Mns_Var2.class" });

		WebResource webResource = getWebResource(Paths.SCRIPT_UPDATE_SERVICE);
		String updatedScriptBody = IOHelper.loadScript("duplicated-expression-script-upd.txt");
		ScriptAdminParams script = new ScriptAdminParams("ScriptToUpdate", updatedScriptBody);

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		assertTrue(scriptDir.isDirectory());
		assertEquals(6, scriptDir.list().length);
		// Additional ScriptToUpdate$$3.class is persisted, as the expression "1" (that was duplicated) is replaced with
		// new expression "3".
		// ScriptToUpdate$$Var1_Mns_Var2.class is replaced with ScriptToUpdate$$Var1_Mns_Var3.class, as Var2 is replaced
		// with Var3 in "duplicated-expression-script-upd.txt" script
		assertFilesExist(scriptDir.getPath(), new String[] { "ScriptToUpdate.class", "ScriptToUpdate.src",
				"ScriptToUpdate$$3.class", "ScriptToUpdate$$1.class", "ScriptToUpdate$$Var1_Mns_Var3.class" });

		String actualScriptBody = IOHelper.loadScript(scriptDir.getPath(), "ScriptToUpdate.src");
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
		assertFalse(new File(FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH
				+ "/nonexistentscript").exists());
	}

	@Test
	public void sourceCodeForExistingScriptIsLoaded() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_GET_SOURCE_SERVICE);
		ScriptAdminParams script = new ScriptAdminParams();
		script.setName("ScriptToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		String expectedSource = IOHelper.loadScript(
				FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH + "/scripttoread",
				"ScriptToRead.src");

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
	public void validScriptIsRun() throws Exception {
		WebResource webResource = getWebResource(Paths.SCRIPT_RUN_SERVICE);
		ScriptRuntimeParams script = new ScriptRuntimeParams();
		script.setName("ScriptToRead");

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());

		RuntimeStatus status = response.getEntity(RuntimeStatus.class);
		assertTrue(status.getRuntimeId() > 0);
		assertEquals(RuntimeStatusType.COMPLETED, status.getStatusType());
	}

	private void assertFilesExist(String dirPath, String[] fileNames) {
		for (String fileName : fileNames) {
			String filePath = dirPath + "/" + fileName;
			assertTrue("Expected file [" + filePath + "] does not exist", new File(filePath).exists());
		}
	}

	private static class FSRepositoryApplicationConfig extends GuiceServletContextListener {

		private static final String LOGGER_NAME = "test_logger";

		final static String CODE_REPOSITORY_PATH = TestConstants.TEST_RESOURCES_DIR + "/__tmp_code_repository";

		@Override
		protected Injector getInjector() {
			return Guice.createInjector(new JerseyServletModule() {

				@Override
				protected void configureServlets() {
					bind(CodeRepository.class).to(FSCodeRepository.class);

					requestStaticInjection(DynamicClassLoader.class);

					Map<String, String> webConfig = new HashMap<>();
					// org.codehaus.jackson.jaxrs package contains the provider for POJO JSON mapping
					webConfig.put(PackagesResourceConfig.PROPERTY_PACKAGES,
							"com.ilsid.bfa.service.server; org.codehaus.jackson.jaxrs");
					webConfig.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE.toString());

					serve("/*").with(GuiceContainer.class, webConfig);
				}

				@Provides
				@RepositoryConfig
				protected Map<String, String> provideRepositoryConfiguration() {
					Map<String, String> result = new HashMap<>();
					result.put("bfa.persistence.fs.root_dir", CODE_REPOSITORY_PATH);

					return result;
				}

				@Provides
				@WebAppLogger
				protected Logger provideLogger() {
					return LoggerFactory.getLogger(LOGGER_NAME);
				}
			});
		}

	}

}
