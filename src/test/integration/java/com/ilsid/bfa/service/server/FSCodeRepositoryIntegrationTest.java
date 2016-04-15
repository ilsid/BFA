package com.ilsid.bfa.service.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ilsid.bfa.IntegrationTestConstants;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.filesystem.FilesystemActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.LoggingConfigurator;
import com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository;

public abstract class FSCodeRepositoryIntegrationTest extends RESTServiceIntegrationTestCase {

	private static final String LOGGING_CONFIG_FILE = TestConstants.TEST_RESOURCES_DIR + "/test-log4j.xml";

	private static final String GENERATED_ENTITY_ROOT_PATH = "com/ilsid/bfa/generated/entity";

	private static final String GENERATED_ENTITY_DEFAULT_GROUP_DIR = GENERATED_ENTITY_ROOT_PATH + "/"
			+ ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE;

	protected static final String CODE_REPOSITORY_PATH = IntegrationTestConstants.CODE_REPOSITORY_DIR.getPath();

	protected static final File ENTITY_REPOSITORY_ROOT_DIR = new File(IntegrationTestConstants.CODE_REPOSITORY_DIR,
			GENERATED_ENTITY_ROOT_PATH);

	protected static final File ENTITY_REPOSITORY_DEFAULT_GROUP_DIR_PATH = new File(
			IntegrationTestConstants.CODE_REPOSITORY_DIR, GENERATED_ENTITY_DEFAULT_GROUP_DIR);

	@BeforeClass
	public static void setUp() throws Exception {
		FileUtils.forceMkdir(IntegrationTestConstants.CODE_REPOSITORY_DIR);
		LoggingConfigurator.configureLog4j(LOGGING_CONFIG_FILE);

		// Creating the "pre-condition" scripts required for some tests
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/integration_tests/code_repository"),
				IntegrationTestConstants.CODE_REPOSITORY_DIR);

		Map<String, String> repositoryConfig = new HashMap<>();
		repositoryConfig.put("bfa.persistence.fs.root_dir", IntegrationTestConstants.CODE_REPOSITORY_DIR.getPath());
		repositoryConfig.put("bfa.persistence.orientdb.url", TestConstants.DATABASE_URL);
		repositoryConfig.put("bfa.persistence.orientdb.user", TestConstants.DATABASE_USER);
		repositoryConfig.put("bfa.persistence.orientdb.password", TestConstants.DATABASE_PASSWORD);

		startDatabaseServer();

		startWebServer(new TestApplicationConfig(FilesystemScriptingRepository.class, FilesystemActionRepository.class,
				repositoryConfig));
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopWebServer();
		stopDatabaseServer();
		FileUtils.forceDelete(IntegrationTestConstants.CODE_REPOSITORY_DIR);
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
				ENTITY_REPOSITORY_DEFAULT_GROUP_DIR_PATH);
	}

	protected void copyFileFromEntityDirToRepository(String entityDir, String fileName) throws Exception {
		String entitySourceDir = "/integration_tests/to_copy/com/ilsid/bfa/generated/entity/" + entityDir;
		FileUtils.copyFileToDirectory(new File(TestConstants.TEST_RESOURCES_DIR + entitySourceDir + "/" + fileName),
				new File(ENTITY_REPOSITORY_ROOT_DIR, entityDir));
	}

	protected void copyEntityDirectoryToRepository(String sourceDir, String destDir) throws Exception {
		String entityActualDir = "/integration_tests/to_copy/com/ilsid/bfa/generated/entity/" + sourceDir;
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + entityActualDir),
				new File(ENTITY_REPOSITORY_ROOT_DIR, destDir));
	}

	protected void deleteFileFromEntityRepository(String filePath) throws Exception {
		FileUtils.forceDelete(new File(ENTITY_REPOSITORY_ROOT_DIR, filePath));
	}

	protected void copyDirectoryToRepository(String sourceDir, String destDir) throws Exception {
		FileUtils.copyDirectory(new File(sourceDir), new File(IntegrationTestConstants.CODE_REPOSITORY_DIR, destDir));
	}

	protected Map<String, String> loadMetadata(File metaFile) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, String> result = new ObjectMapper().readValue(metaFile, Map.class);
		return result;
	}

}
