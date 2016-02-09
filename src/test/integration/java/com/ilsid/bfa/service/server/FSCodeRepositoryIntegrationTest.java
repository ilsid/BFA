package com.ilsid.bfa.service.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.LoggingConfigurator;
import com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository;

public abstract class FSCodeRepositoryIntegrationTest extends RESTServiceIntegrationTestCase {

	private static final String LOGGING_CONFIG_FILE = TestConstants.TEST_RESOURCES_DIR + "/test-log4j.xml";

	protected static final String CODE_REPOSITORY_PATH = TestConstants.TEST_RESOURCES_DIR + "/__tmp_code_repository";

	protected static final File CODE_REPOSITORY_DIR = new File(CODE_REPOSITORY_PATH);

	protected static final String GENERATED_ENTITY_ROOT_PATH = "com/ilsid/bfa/generated/entity";

	protected static final String GENERATED_ENTITY_DEFAULT_GROUP_PATH = GENERATED_ENTITY_ROOT_PATH + "/"
			+ ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE;

	protected static final String ENTITY_REPOSITORY_ROOT_DIR_PATH = CODE_REPOSITORY_PATH + "/"
			+ GENERATED_ENTITY_ROOT_PATH;

	protected static final String ENTITY_REPOSITORY_DEFAULT_GROUP_DIR_PATH = CODE_REPOSITORY_PATH + "/"
			+ GENERATED_ENTITY_DEFAULT_GROUP_PATH;

	@BeforeClass
	public static void setUp() throws Exception {
		FileUtils.forceMkdir(CODE_REPOSITORY_DIR);
		LoggingConfigurator.configureLog4j(LOGGING_CONFIG_FILE);

		// Creating the "pre-condition" scripts required for some tests
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/integration_tests/code_repository"),
				CODE_REPOSITORY_DIR);

		Map<String, String> repositoryConfig = new HashMap<>();
		repositoryConfig.put("bfa.persistence.fs.root_dir", CODE_REPOSITORY_PATH);

		startWebServer(new TestApplicationConfig(FilesystemScriptingRepository.class, repositoryConfig));
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

	protected void copyFileFromEntityDefaulGroupDirToRepository(String fileName) throws Exception {
		String entitySourceDir = "/integration_tests/to_copy/com/ilsid/bfa/generated/entity/default_group";
		FileUtils.copyFileToDirectory(new File(TestConstants.TEST_RESOURCES_DIR + entitySourceDir + "/" + fileName),
				new File(ENTITY_REPOSITORY_DEFAULT_GROUP_DIR_PATH));
	}

	protected void copyFileFromEntityDirToRepository(String entityDir, String fileName) throws Exception {
		String entitySourceDir = "/integration_tests/to_copy/com/ilsid/bfa/generated/entity/" + entityDir;
		FileUtils.copyFileToDirectory(new File(TestConstants.TEST_RESOURCES_DIR + entitySourceDir + "/" + fileName),
				new File(ENTITY_REPOSITORY_ROOT_DIR_PATH + "/" + entityDir));
	}
	
	protected void deleteFileFromEntityRepository(String filePath) throws Exception {
		FileUtils.forceDelete(new File(ENTITY_REPOSITORY_ROOT_DIR_PATH + "/" + filePath));
	}

	protected void copyDirectoryToRepository(String sourceDir, String destDir) throws Exception {
		FileUtils.copyDirectory(new File(sourceDir), new File(CODE_REPOSITORY_PATH + "/" + destDir));
	}

	protected Map<String, String> loadMetadata(File metaFile) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, String> result = new ObjectMapper().readValue(metaFile, Map.class);
		return result;
	}

}
