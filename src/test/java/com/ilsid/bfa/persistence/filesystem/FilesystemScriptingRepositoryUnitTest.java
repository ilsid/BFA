package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.CompileHelper;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.ScriptingRepository;

public class FilesystemScriptingRepositoryUnitTest extends BaseUnitTestCase {

	private static final File CODE_REPOSITORY_SOURCE_DIR = new File(TestConstants.CODE_REPOSITORY_DIR);

	private static final String SCRIPT_SOURCE_FILE_NAME = "single-expression-script.txt";

	private final static String ROOT_DIR_PATH = TestConstants.TEST_RESOURCES_DIR + "/__tmp_class_repository";

	private final static String SCRIPT_CLASS_NAME = CompileHelper.GENERATED_SCRIPT_PACKAGE
			+ FilesystemScriptingRepositoryUnitTest.class.getSimpleName() + "Script01";

	private final static String PATH_WO_EXTENSION = ROOT_DIR_PATH + "/" + SCRIPT_CLASS_NAME.replace('.', '/');

	private final static File SAVED_SCRIPT_CLASS_FILE = new File(PATH_WO_EXTENSION + ".class");

	private final static File SAVED_SCRIPT_SRC_FILE = new File(PATH_WO_EXTENSION + ".src");

	private final static File ROOT_DIR = new File(ROOT_DIR_PATH);

	private ScriptingRepository repository = new FilesystemScriptingRepository();

	@Before
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		FileUtils.forceMkdir(ROOT_DIR);

		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", ROOT_DIR_PATH);
			}
		});
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.forceDelete(ROOT_DIR);
	}

	@Test
	public void defaultScripGroupMetadataFileExists() throws Exception {
		String metaData = IOHelper.loadFileContents(
				ROOT_DIR_PATH + "/" + ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE.replace('.', '/'),
				ClassNameUtil.METADATA_FILE_NAME);

		assertEquals("{\"type\":\"SCRIPT_GROUP\",\"name\":\"default_group\",\"title\":\"Default Group\"}", metaData);
	}

	@Test
	public void classOnlyCanBeSaved() throws Exception {
		saveClass();

		assertTrue(SAVED_SCRIPT_CLASS_FILE.exists());
		assertFalse(SAVED_SCRIPT_SRC_FILE.exists());
	}

	@Test
	public void classAndSourceCodeCanBeSaved() throws Exception {
		saveClassAndSource();

		assertTrue(SAVED_SCRIPT_CLASS_FILE.exists());
		assertTrue(SAVED_SCRIPT_SRC_FILE.exists());

		String savedScriptBodySource;
		try (InputStream savedScriptBody = new FileInputStream(SAVED_SCRIPT_SRC_FILE)) {
			savedScriptBodySource = IOUtils.toString(savedScriptBody, "UTF-8");
		}
		String inputScriptBodySource = IOHelper.loadScript(SCRIPT_SOURCE_FILE_NAME);

		assertEquals(inputScriptBodySource, savedScriptBodySource);
	}

	@Test
	public void classOnlySaveFailsIfConfigurationIsNotSet() throws Exception {
		expectExceptionIfConfigIsNotSet();
		saveClass();
	}

	@Test
	public void classAndSourceSaveFailsIfConfigurationIsNotSet() throws Exception {
		expectExceptionIfConfigIsNotSet();
		saveClassAndSource();
	}

	@Test
	public void rootDirectoryPropertyIsRequiredInConfiguration() throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule.expectMessage("Required [bfa.persistence.fs.root_dir] property not found");

		ScriptingRepository rep = new FilesystemScriptingRepository();
		rep.setConfiguration(new HashMap<String, String>());
	}

	@Test
	@SuppressWarnings("serial")
	public void existingDirectoryValueIsRequiredInConfiguration() throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule.expectMessage(
				"[src/test/resources/non-existent-dir] value defined by [bfa.persistence.fs.root_dir] property is not a directory");

		ScriptingRepository rep = new FilesystemScriptingRepository();
		rep.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", "src/test/resources/non-existent-dir");
			}
		});
	}

	@Test
	public void classOnlySaveFailsIfClassWithSuchNameAlreadyExists() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage(
				"Class [com.ilsid.bfa.generated.script.FilesystemScriptingRepositoryUnitTestScript01] already exists in directory src/test/resources/__tmp_class_repository");

		saveClass();
		saveClass();
	}

	@Test
	public void classAndSourceSaveFailsIfClassWithSuchNameAlreadyExists() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage(
				"Class [com.ilsid.bfa.generated.script.FilesystemScriptingRepositoryUnitTestScript01] already exists in directory src/test/resources/__tmp_class_repository");

		saveClassAndSource();
		saveClassAndSource();
	}

	@Test
	public void existingEmptyPackageCanBeDeleted() throws Exception {
		String packageDirPath = ROOT_DIR_PATH + "/" + "com/test/pkg";
		File packageDir = new File(packageDirPath);

		FileUtils.forceMkdir(packageDir);
		assertTrue(packageDir.exists());

		int delCnt = repository.deletePackage("com.test.pkg");
		assertEquals(0, delCnt);
		assertFalse(packageDir.exists());
	}

	@Test
	public void existingPackageWithClassesCanBeDeleted() throws Exception {
		String packageDirPath = ROOT_DIR_PATH + "/" + "com/test/pkg";
		File packageDir = new File(packageDirPath);

		FileUtils.forceMkdir(packageDir);
		assertTrue(packageDir.exists());

		new File(packageDirPath + "/file1.txt").createNewFile();
		new File(packageDirPath + "/file2.txt").createNewFile();

		assertEquals(2, packageDir.list().length);

		int delCnt = repository.deletePackage("com.test.pkg");
		assertEquals(2, delCnt);
		assertFalse(packageDir.exists());
	}

	@Test
	public void nonExistingPackageIsIgnoredWhenDeleting() throws Exception {
		String packageDirPath = ROOT_DIR_PATH + "/" + "com/test/not_exist/pkg";
		File packageDir = new File(packageDirPath);

		assertFalse(packageDir.exists());

		int delCnt = repository.deletePackage("com.test.not_exist.pkg");
		assertEquals(0, delCnt);
	}

	@Test
	public void sourceCodeForExistingScriptCanBeLoaded() throws Exception {
		createCodeRepository();

		String scriptSource = repository
				.loadSourceCode("com.ilsid.bfa.generated.script.default_group.script001.Script001");
		String expectedSource = IOHelper.loadFileContents(TestConstants.TEST_RESOURCES_DIR
				+ "/code_repository/com/ilsid/bfa/generated/script/default_group/script001", "Script001.src");

		assertEquals(expectedSource, scriptSource);
	}

	@Test
	public void noSourceCodeIsLoadedforNonExistentScript() throws Exception {
		assertNull(repository.loadSourceCode("some.nonexistent.script.Script001"));
	}

	@Test
	public void byteCodeForExistingScriptCanBeLoaded() throws Exception {
		createCodeRepository();

		byte[] byteCode = repository.load("com.ilsid.bfa.generated.script.default_group.script001.Script001");
		byte[] expectedByteCode = IOHelper.loadClass(TestConstants.TEST_RESOURCES_DIR
				+ "/code_repository/com/ilsid/bfa/generated/script/default_group/script001", "Script001.class");

		assertTrue(Arrays.equals(expectedByteCode, byteCode));
	}

	@Test
	public void noByteCodeIsLoadedforNonExistentScript() throws Exception {
		assertNull(repository.load("some.nonexistent.script.Script001"));
	}

	@Test
	public void classAndItsSourceCanBeDeleted() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.script001.Script001";
		String filePrefix = ROOT_DIR_PATH + "/" + className.replace('.', '/');
		File classFile = new File(filePrefix + ".class");
		File sourceFile = new File(filePrefix + ".src");
		assertTrue(classFile.exists());
		assertTrue(sourceFile.exists());

		int deletedCount = repository.deleteClass(className);
		assertEquals(2, deletedCount);
		assertFalse(classFile.exists());
		assertFalse(classFile.exists());
	}

	@Test
	public void classWithoutSourceCanBeDeleted() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.script001.Script001$$1";
		String filePrefix = ROOT_DIR_PATH + "/" + className.replace('.', '/');
		File classFile = new File(filePrefix + ".class");
		File sourceFile = new File(filePrefix + ".src");
		assertTrue(classFile.exists());
		assertFalse(sourceFile.exists());

		int deletedCount = repository.deleteClass(className);
		assertEquals(1, deletedCount);
		assertFalse(classFile.exists());
	}

	@Test
	public void nonExistingClassIsNotDeletedSilently() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.script001.SomeNonExistingScript";
		String filePrefix = ROOT_DIR_PATH + "/" + className.replace('.', '/');
		File classFile = new File(filePrefix + ".class");
		File sourceFile = new File(filePrefix + ".src");
		assertFalse(classFile.exists());
		assertFalse(sourceFile.exists());

		int deletedCount = repository.deleteClass(className);
		assertEquals(0, deletedCount);
	}

	@Test
	public void metadataForExistingClassCanBeSaved() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.script001.Script001";

		boolean result = repository.saveMetadata(className, createMetadata());
		assertTrue(result);

		String savedMetadata = IOHelper.loadFileContents(
				ROOT_DIR_PATH + "/" + "com.ilsid.bfa.generated.script.default_group.script001".replace('.', '/'),
				ClassNameUtil.METADATA_FILE_NAME);

		assertEquals("{\"type\":\"SCRIPT\",\"name\":\"Script001\",\"title\":\"Script 001\"}", savedMetadata);
	}

	@Test
	public void metadataForNonExistingClassCanNotBeSaved() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.script001.SomeNonExistingScript";

		boolean result = repository.saveMetadata(className, createMetadata());
		assertFalse(result);
		assertFalse(new File(
				ROOT_DIR_PATH + "/" + "com.ilsid.bfa.generated.script.default_group.script001".replace('.', '/') + "/"
						+ ClassNameUtil.METADATA_FILE_NAME).exists());
	}

	@Test
	public void metadataForDefaultScriptGroupCanBeLoaded() throws Exception {
		List<Map<String, String>> metaDatas = repository.loadGroupMetadatas();

		assertEquals(1, metaDatas.size());
		final Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.NAME));
		assertEquals(Metadata.DEFAULT_GROUP_TITLE, metaData.get(Metadata.TITLE));
	}

	private void createCodeRepository() throws Exception {
		FileUtils.copyDirectory(CODE_REPOSITORY_SOURCE_DIR, ROOT_DIR);
	}

	private void saveClassAndSource() throws Exception {
		saveClass(true);
	}

	private void saveClass() throws Exception {
		saveClass(false);
	}

	private void expectExceptionIfConfigIsNotSet() {
		exceptionRule.expect(IllegalStateException.class);
		exceptionRule.expectMessage("Root directory is not set");

		repository = new FilesystemScriptingRepository();
	}

	private void saveClass(boolean saveSource) throws Exception {
		String scriptBody = IOHelper.loadScript(SCRIPT_SOURCE_FILE_NAME);
		byte[] byteCode = CompileHelper.compileScript(SCRIPT_CLASS_NAME, IOUtils.toInputStream(scriptBody));

		if (saveSource) {
			repository.save(SCRIPT_CLASS_NAME, byteCode, scriptBody);
		} else {
			repository.save(SCRIPT_CLASS_NAME, byteCode);
		}
	}

	private Map<String, String> createMetadata() {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put("type", "SCRIPT");
		metaData.put("name", "Script001");
		metaData.put("title", "Script 001");

		return metaData;
	}

}
