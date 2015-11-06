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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.servlet.GuiceServletContextListener;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.RepositoryConfig;
import com.ilsid.bfa.persistence.filesystem.FSCodeRepository;
import com.ilsid.bfa.service.dto.ScriptDTO;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class ScriptResourceWithFSRepositoryIntegrationTest extends RESTServiceIntegrationTestCase {

	private static final String GENERATED_SCRIPT_ROOT_PATH = "com/ilsid/bfa/generated/script/";

	private static final File CODE_REPOSITORY_DIR = new File(FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH);

	@BeforeClass
	public static void setUp() throws Exception {
		FileUtils.forceMkdir(CODE_REPOSITORY_DIR);
		startWebServer(new FSRepositoryApplicationConfig());
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopWebServer();
		FileUtils.forceDelete(CODE_REPOSITORY_DIR);
	}

	@Test
	public void validScriptIsCompiledAndItsSourceAndAllClassesAreSavedInFileSystem() throws Exception {
		WebResource webResource = getWebResource("script/create");
		ScriptDTO script = new ScriptDTO("Script001", IOHelper.loadScript("duplicated-expression-script.txt"));
		// ScriptDTO script = new ScriptDTO("Script001", IOHelper.loadScript("two-invalid-expressions-script.txt"));

		ClientResponse response = webResource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, script);

		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		assertFilesExist("script001", new String[] { "Script001.class", "Script001.src", "Script001$$1.class",
				"Script001$$Var1_Mns_Var2.class" });
	}

	private void assertFilesExist(String scriptPackage, String[] fileNames) {
		for (String fileName : fileNames) {
			String filePath = FSRepositoryApplicationConfig.CODE_REPOSITORY_PATH + "/" + GENERATED_SCRIPT_ROOT_PATH
					+ scriptPackage + "/" + fileName;
			assertTrue("Expected file [" + filePath + "] does not exist", new File(filePath).exists());
		}
	}

	private static class FSRepositoryApplicationConfig extends GuiceServletContextListener {

		final static String CODE_REPOSITORY_PATH = "src/test/resources/__tmp_code_repository";

		@Override
		protected Injector getInjector() {
			return Guice.createInjector(new JerseyServletModule() {

				@Override
				protected void configureServlets() {
					bind(CodeRepository.class).to(FSCodeRepository.class);

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
			});
		}

	}

}
