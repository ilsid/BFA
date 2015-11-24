package com.ilsid.bfa.service.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.common.LoggingConfigurator;
import com.ilsid.bfa.persistence.filesystem.FSCodeRepository;

public class FSCodeRepositoryIntegrationTest extends RESTServiceIntegrationTestCase {

	private static final String LOGGING_CONFIG_FILE = TestConstants.TEST_RESOURCES_DIR + "/log4j.xml";

	protected static final String CODE_REPOSITORY_PATH = TestConstants.TEST_RESOURCES_DIR + "/__tmp_code_repository";

	protected static final File CODE_REPOSITORY_DIR = new File(CODE_REPOSITORY_PATH);

	@BeforeClass
	public static void setUp() throws Exception {
		FileUtils.forceMkdir(CODE_REPOSITORY_DIR);
		LoggingConfigurator.configureLog4j(LOGGING_CONFIG_FILE);

		// Creating the "pre-condition" scripts required for some tests
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/integration_tests/code_repository"),
				CODE_REPOSITORY_DIR);

		Map<String, String> repositoryConfig = new HashMap<>();
		repositoryConfig.put("bfa.persistence.fs.root_dir", CODE_REPOSITORY_PATH);

		startWebServer(new TestApplicationConfig(FSCodeRepository.class, repositoryConfig));
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopWebServer();
		FileUtils.forceDelete(CODE_REPOSITORY_DIR);
	}

	protected void assertFilesExist(String dirPath, String[] fileNames) {
		for (String fileName : fileNames) {
			String filePath = dirPath + "/" + fileName;
			assertTrue("Expected file [" + filePath + "] does not exist", new File(filePath).exists());
		}
	}

}
