package com.ilsid.bfa.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.ConfigurationException;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.common.CompileHelper;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;

public class FSCodeRepositoryUnitTest extends BaseUnitTestCase {

	private static final String SCRIPT_SOURCE_FILE_NAME = "single-expression-script.txt";

	private final static String ROOT_DIR_PATH = TestConstants.TEST_RESOURCES_DIR + "/__tmp_class_repository";

	private final static String SCRIPT_CLASS_NAME = CompileHelper.GENERATED_SCRIPT_PACKAGE
			+ FSCodeRepositoryUnitTest.class.getSimpleName() + "Script01";

	private final static String PATH_WO_EXTENSION = ROOT_DIR_PATH + "/" + SCRIPT_CLASS_NAME.replace('.', '/');

	private final static File SAVED_SCRIPT_CLASS_FILE = new File(PATH_WO_EXTENSION + ".class");

	private final static File SAVED_SCRIPT_SRC_FILE = new File(PATH_WO_EXTENSION + ".src");

	private final static File ROOT_DIR = new File(ROOT_DIR_PATH);

	private CodeRepository repository = new FSCodeRepository();

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

		CodeRepository rep = new FSCodeRepository();
		rep.setConfiguration(new HashMap<String, String>());
	}

	@Test
	@SuppressWarnings("serial")
	public void existingDirectoryValueIsRequiredInConfiguration() throws Exception {
		exceptionRule.expect(ConfigurationException.class);
		exceptionRule.expectMessage(
				"[src/test/resources/non-existent-dir] value defined by [bfa.persistence.fs.root_dir] property is not a directory");

		CodeRepository rep = new FSCodeRepository();
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
				"Class [com.ilsid.bfa.generated.script.FSCodeRepositoryUnitTestScript01] already exists in directory src/test/resources/__tmp_class_repository");

		saveClass();
		saveClass();
	}

	@Test
	public void classAndSourceSaveFailsIfClassWithSuchNameAlreadyExists() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage(
				"Class [com.ilsid.bfa.generated.script.FSCodeRepositoryUnitTestScript01] already exists in directory src/test/resources/__tmp_class_repository");

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
	
	private void saveClassAndSource() throws Exception {
		saveClass(true);
	}

	private void saveClass() throws Exception {
		saveClass(false);
	}

	private void expectExceptionIfConfigIsNotSet() {
		exceptionRule.expect(IllegalStateException.class);
		exceptionRule.expectMessage("Root directory is not set");

		repository = new FSCodeRepository();
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

}
