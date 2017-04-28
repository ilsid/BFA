package com.ilsid.bfa.service.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ilsid.bfa.IntegrationTestConstants;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.filesystem.FilesystemActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.LoggingConfigurator;
import com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository;
import com.ilsid.bfa.runtime.monitor.MonitoringServer;

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
		if (IntegrationTestConstants.CODE_REPOSITORY_DIR.exists()) {
			FileUtils.forceDelete(IntegrationTestConstants.CODE_REPOSITORY_DIR);
		}
		FileUtils.forceMkdir(IntegrationTestConstants.CODE_REPOSITORY_DIR);
		
		LoggingConfigurator.configureLog4j(LOGGING_CONFIG_FILE);

		// Creating the "pre-condition" scripts required for some tests
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/integration_tests/code_repository"),
				IntegrationTestConstants.CODE_REPOSITORY_DIR);

		Map<String, String> repositoryConfig = new HashMap<>();
		repositoryConfig.put("bfa.persistence.fs.root_dir", IntegrationTestConstants.CODE_REPOSITORY_DIR.getPath());
		repositoryConfig.put("bfa.tmp_dir", IntegrationTestConstants.CODE_REPOSITORY_DIR.getPath());
		repositoryConfig.putAll(getDatabaseServerManager().getConfig());
		repositoryConfig.put("bfa.monitor.server.port", IntegrationTestConstants.MONITORING_SERVER_PORT);

		getDatabaseServerManager().startServer();

		startWebServer(new TestApplicationConfig(FilesystemScriptingRepository.class, FilesystemActionRepository.class,
				repositoryConfig));
	}

	@AfterClass
	public static void tearDown() throws Exception {
		stopWebServer();
		getDatabaseServerManager().stopServer();
		FileUtils.forceDelete(IntegrationTestConstants.CODE_REPOSITORY_DIR);
		MonitoringServer.stop();
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

	protected void copyEntityDirectoryToRepository(String dir) throws Exception {
		String entityActualDir = "/integration_tests/to_copy/com/ilsid/bfa/generated/entity/" + dir;
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + entityActualDir),
				new File(ENTITY_REPOSITORY_ROOT_DIR, dir));
	}

	protected void deleteFileFromEntityRepository(String filePath) throws Exception {
		FileUtils.forceDelete(new File(ENTITY_REPOSITORY_ROOT_DIR, filePath));
	}

	protected void copyDirectoryToRepository(String sourceDir, String destDir) throws Exception {
		FileUtils.copyDirectory(new File(sourceDir), new File(IntegrationTestConstants.CODE_REPOSITORY_DIR, destDir));
	}

	protected Map<String, String> loadMetadata(File metaFile) throws Exception {
		return IOHelper.loadMetadata(metaFile);
	}

	protected long getRepositoryVersion() throws Exception {
		return Long.parseLong(IOHelper.loadFileContents(CODE_REPOSITORY_PATH, ".version"));
	}

	protected void assertIncrementedVersion(long oldVersion) throws Exception {
		long newVersion = getRepositoryVersion();
		assertEquals(1L, newVersion - oldVersion);
	}

	protected void assertSameVersion(long oldVersion) throws Exception {
		long newVersion = getRepositoryVersion();
		assertEquals(oldVersion, newVersion);
	}

}
