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

	private static final String SCRIPT_GENERATED_SOURCE_FILE_NAME = "dummy-generated-script.txt";

	private static final String SCRIPT_DIAGRAM_FILE_NAME = "dummy-flow-diagram.txt";

	private final static String SCRIPT_CLASS_NAME = CompileHelper.GENERATED_SCRIPT_PACKAGE
			+ FilesystemScriptingRepositoryUnitTest.class.getSimpleName() + "Script01";

	private final static String PATH_WO_EXTENSION = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/"
			+ SCRIPT_CLASS_NAME.replace('.', '/');

	private final static File SAVED_SCRIPT_CLASS_FILE = new File(PATH_WO_EXTENSION + ".class");

	private final static File SAVED_SCRIPT_SRC_FILE = new File(PATH_WO_EXTENSION + ".src");

	private final static File SAVED_SCRIPT_GENERATED_SRC_FILE = new File(PATH_WO_EXTENSION + "_generated.src");

	private final static File SAVED_SCRIPT_DIAGRAM_FILE = new File(PATH_WO_EXTENSION + ".dgm");

	private final static String COMMON_LIB_DIR_PATH = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/common_lib";

	private ScriptingRepository repository = new FilesystemScriptingRepository();

	@Before
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		FileUtils.forceMkdir(REPOSITORY_ROOT_DIR);

		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.REPOSITORY_ROOT_DIR_PATH);
				put("bfa.persistence.fs.common_lib_dir", COMMON_LIB_DIR_PATH);
			}
		});
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.forceDelete(REPOSITORY_ROOT_DIR);
	}

	@Test
	public void defaultScriptGroupMetadataFileExists() throws Exception {
		String metaData = IOHelper.loadFileContents(
				TestConstants.REPOSITORY_ROOT_DIR_PATH + "/"
						+ ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE.replace('.', '/'),
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
	public void classAndSourceCodeAndDiagramCanBeSaved() throws Exception {
		saveClassAndSourceAndDiagram();

		assertTrue(SAVED_SCRIPT_CLASS_FILE.exists());
		assertTrue(SAVED_SCRIPT_SRC_FILE.exists());
		assertTrue(SAVED_SCRIPT_GENERATED_SRC_FILE.exists());
		assertTrue(SAVED_SCRIPT_DIAGRAM_FILE.exists());

		String savedScriptBodySource;
		try (InputStream savedScriptBody = new FileInputStream(SAVED_SCRIPT_SRC_FILE)) {
			savedScriptBodySource = IOUtils.toString(savedScriptBody, "UTF-8");
		}
		String expectedScriptBodySource = IOHelper.loadScript(SCRIPT_SOURCE_FILE_NAME);

		assertEquals(expectedScriptBodySource, savedScriptBodySource);

		String savedScriptGeneratedSource;
		try (InputStream savedGeneratedCode = new FileInputStream(SAVED_SCRIPT_GENERATED_SRC_FILE)) {
			savedScriptGeneratedSource = IOUtils.toString(savedGeneratedCode, "UTF-8");
		}
		String expectedScriptGeneratedSource = IOHelper.loadScript(SCRIPT_GENERATED_SOURCE_FILE_NAME);

		assertEquals(expectedScriptGeneratedSource, savedScriptGeneratedSource);

		String savedScriptDiagram;
		try (InputStream savedDiagram = new FileInputStream(SAVED_SCRIPT_DIAGRAM_FILE)) {
			savedScriptDiagram = IOUtils.toString(savedDiagram, "UTF-8");
		}
		String expectedScriptDiagram = IOHelper.loadScript(SCRIPT_DIAGRAM_FILE_NAME);

		assertEquals(expectedScriptDiagram, savedScriptDiagram);
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

		saveClassAndSourceAndDiagram();
		saveClassAndSourceAndDiagram();
	}

	@Test
	public void existingEmptyPackageCanBeDeleted() throws Exception {
		String packageDirPath = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + "com/test/pkg";
		File packageDir = new File(packageDirPath);

		FileUtils.forceMkdir(packageDir);
		assertTrue(packageDir.exists());

		int delCnt = repository.deletePackage("com.test.pkg");
		assertEquals(0, delCnt);
		assertFalse(packageDir.exists());
	}

	@Test
	public void existingPackageWithClassesCanBeDeleted() throws Exception {
		String packageDirPath = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + "com/test/pkg";
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
		String packageDirPath = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + "com/test/not_exist/pkg";
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
	public void diagramForExistingScriptCanBeLoaded() throws Exception {
		createCodeRepository();

		String diagram = repository.loadDiagram("com.ilsid.bfa.generated.script.default_group.script001.Script001");
		String expectedDiagram = IOHelper.loadFileContents(TestConstants.TEST_RESOURCES_DIR
				+ "/code_repository/com/ilsid/bfa/generated/script/default_group/script001", "Script001.dgm");

		assertEquals(expectedDiagram, diagram);
	}

	@Test
	public void noDiagramIsLoadedforNonExistentScript() throws Exception {
		assertNull(repository.loadDiagram("some.nonexistent.script.Script001"));
	}

	@Test
	public void classesForExistingPackageCanBeLoaded() throws Exception {
		createCodeRepository();

		List<Map.Entry<String, byte[]>> classes = repository.loadClasses("com.ilsid.bfa.generated.entity");
		assertEquals(3, classes.size());

		Map<String, byte[]> loadedClasses = new HashMap<>();
		for (Map.Entry<String, byte[]> entry : classes) {
			loadedClasses.put(entry.getKey(), entry.getValue());
		}

		final String className1 = "com.ilsid.bfa.generated.entity.default_group.Entity001";
		final String className2 = "com.ilsid.bfa.generated.entity.default_group.Contract";
		final String className3 = "com.ilsid.bfa.generated.entity.default_group.MSISDN";

		assertTrue(loadedClasses.containsKey(className1));
		assertTrue(loadedClasses.containsKey(className2));
		assertTrue(loadedClasses.containsKey(className3));

		final String entityDir = TestConstants.TEST_RESOURCES_DIR
				+ "/code_repository/com/ilsid/bfa/generated/entity/default_group";

		assertTrue(Arrays.equals(IOHelper.loadClass(entityDir, "Entity001.class"), loadedClasses.get(className1)));
		assertTrue(Arrays.equals(IOHelper.loadClass(entityDir, "Contract.class"), loadedClasses.get(className2)));
		assertTrue(Arrays.equals(IOHelper.loadClass(entityDir, "MSISDN.class"), loadedClasses.get(className3)));
	}

	@Test
	public void noClassesAreLoadedForNonExistingPackage() throws Exception {
		createCodeRepository();

		assertEquals(0, repository.loadClasses("com.ilsid.bfa.generated.non_existing_pkg").size());
	}

	@Test
	public void classAndItsSourceCanBeDeleted() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.script001.Script001";
		String filePrefix = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + className.replace('.', '/');
		File classFile = new File(filePrefix + ".class");
		File sourceFile = new File(filePrefix + ".src");
		assertTrue(classFile.exists());
		assertTrue(sourceFile.exists());

		int deletedCount = repository.deleteClass(className);
		assertEquals(2, deletedCount);
		assertFalse(classFile.exists());
		assertFalse(sourceFile.exists());
	}

	@Test
	public void classAndItsSourceAndMetadataCanBeDeleted() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.entity.default_group.Entity001";
		String filePrefix = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + className.replace('.', '/');
		File classFile = new File(filePrefix + ".class");
		File sourceFile = new File(filePrefix + ".src");
		File metaFile = new File(filePrefix + "_" + ClassNameUtil.METADATA_FILE_NAME);
		assertTrue(classFile.exists());
		assertTrue(sourceFile.exists());
		assertTrue(metaFile.exists());

		int deletedCount = repository.deleteClass(className);
		assertEquals(3, deletedCount);
		assertFalse(classFile.exists());
		assertFalse(sourceFile.exists());
		assertFalse(metaFile.exists());
	}

	@Test
	public void classWithoutSourceCanBeDeleted() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.script002.Script002";
		String filePrefix = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + className.replace('.', '/');
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
		String filePrefix = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + className.replace('.', '/');
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

		String className = "com.ilsid.bfa.generated.entity.default_group.Entity001";

		boolean result = repository.saveMetadata(className, createEntityMetadata());
		assertTrue(result);

		String savedMetadata = IOHelper.loadFileContents(
				TestConstants.REPOSITORY_ROOT_DIR_PATH + "/"
						+ "com.ilsid.bfa.generated.entity.default_group".replace('.', '/'),
				"Entity001_" + ClassNameUtil.METADATA_FILE_NAME);

		assertEquals("{\"type\":\"ENTITY\",\"name\":\"Test_Entity_001\",\"title\":\"Test Entity 001\"}", savedMetadata);
	}

	@Test
	public void metadataForNonExistingClassCanNotBeSaved() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.entity.default_group.SomeNonExistingEntity";

		boolean result = repository.saveMetadata(className, createEntityMetadata());
		assertFalse(result);
		assertFalse(new File(TestConstants.REPOSITORY_ROOT_DIR_PATH + "/"
				+ "com.ilsid.bfa.generated.entity.default_group".replace('.', '/') + "/" + "SomeNonExistingEntity_"
				+ ClassNameUtil.METADATA_FILE_NAME).exists());
	}

	@Test
	public void metadataForExistingPackageCanBeSaved() throws Exception {
		createCodeRepository();

		String packageName = "com.ilsid.bfa.generated.script.default_group.script001";

		boolean result = repository.savePackageMetadata(packageName, createScriptMetadata());
		assertTrue(result);

		String savedMetadata = IOHelper.loadFileContents(
				TestConstants.REPOSITORY_ROOT_DIR_PATH + "/"
						+ "com.ilsid.bfa.generated.script.default_group.script001".replace('.', '/'),
				ClassNameUtil.METADATA_FILE_NAME);

		assertEquals("{\"type\":\"SCRIPT\",\"name\":\"Test_Script_001\",\"title\":\"Test Script 001\"}", savedMetadata);
	}

	@Test
	public void metadataForNonExistingPackageCanNotBeSaved() throws Exception {
		createCodeRepository();

		String className = "com.ilsid.bfa.generated.script.default_group.somenonexistingscript";

		boolean result = repository.savePackageMetadata(className, createScriptMetadata());
		assertFalse(result);
		assertFalse(new File(TestConstants.REPOSITORY_ROOT_DIR_PATH + "/"
				+ "com.ilsid.bfa.generated.script.default_group.somenonexistingscript".replace('.', '/') + "/"
				+ ClassNameUtil.METADATA_FILE_NAME).exists());
	}

	@Test
	public void metadataForTopLevelPackagesCanBeLoaded() throws Exception {
		List<Map<String, String>> metaDatas = repository
				.loadMetadataForChildPackages(ClassNameUtil.GENERATED_SCRIPTS_ROOT_PACKAGE, Metadata.SCRIPT_GROUP_TYPE);

		assertEquals(1, metaDatas.size());
		final Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.NAME));
		assertEquals(Metadata.DEFAULT_GROUP_TITLE, metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForChildPackagesInExistingPackageCanBeLoaded() throws Exception {
		createCodeRepository();

		List<Map<String, String>> metaDatas = repository.loadMetadataForChildPackages(
				ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE, Metadata.SCRIPT_GROUP_TYPE,
				Metadata.SCRIPT_TYPE);

		assertEquals(4, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("custom_group_001", metaData.get(Metadata.NAME));
		assertEquals("Custom Group 001", metaData.get(Metadata.TITLE));

		metaData = metaDatas.get(1);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("custom_group_002", metaData.get(Metadata.NAME));
		assertEquals("Custom Group 002", metaData.get(Metadata.TITLE));

		metaData = metaDatas.get(2);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Script001", metaData.get(Metadata.NAME));
		assertEquals("Script 001", metaData.get(Metadata.TITLE));

		metaData = metaDatas.get(3);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("SingleSubflowScript", metaData.get(Metadata.NAME));
		assertEquals("Single Subflow Script", metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForChildPackagesInNonExistingPackageCanNotLoaded() throws Exception {
		createCodeRepository();
		assertEquals(0,
				repository.loadMetadataForChildPackages(
						ClassNameUtil.GENERATED_SCRIPTS_ROOT_PACKAGE + ".some_non_existing_package",
						Metadata.SCRIPT_GROUP_TYPE, Metadata.SCRIPT_TYPE).size());
	}

	@Test
	public void metadataForClassesInExistingPackageCanBeLoaded() throws Exception {
		createCodeRepository();

		List<Map<String, String>> metaDatas = repository
				.loadMetadataForClasses(ClassNameUtil.GENERATED_ENTITIES_DEFAULT_GROUP_PACKAGE, Metadata.ENTITY_TYPE);

		assertEquals(1, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ENTITY_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Entity001", metaData.get(Metadata.NAME));
		assertEquals("Entity001", metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForClassesInNonExistingPackageCanNotBeLoaded() throws Exception {
		createCodeRepository();
		assertEquals(0,
				repository.loadMetadataForClasses(
						ClassNameUtil.GENERATED_ENTITIES_ROOT_PACKAGE + ".some_non_existing_package",
						Metadata.ENTITY_TYPE).size());
	}

	@Test
	public void metadataForChildPackagesInExistingSubPackageCanBeLoaded() throws Exception {
		createCodeRepository();

		List<Map<String, String>> metaDatas = repository.loadMetadataForChildPackages(
				ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE + ".custom_group_001", Metadata.SCRIPT_GROUP_TYPE,
				Metadata.SCRIPT_TYPE);

		assertEquals(2, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("custom_subgroup_001", metaData.get(Metadata.NAME));
		assertEquals("Custom Sub-Group 001", metaData.get(Metadata.TITLE));

		metaData = metaDatas.get(1);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("custom_subgroup_002", metaData.get(Metadata.NAME));
		assertEquals("Custom Sub-Group 002", metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForExistingPackageCanBeLoaded() throws Exception {
		createCodeRepository();

		Map<String, String> metaData = repository
				.loadMetadataForPackage(ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE + ".custom_group_001");

		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.SCRIPT_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("custom_group_001", metaData.get(Metadata.NAME));
		assertEquals("Custom Group 001", metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForNonExistingPackageCanNotBeLoaded() throws Exception {
		createCodeRepository();

		Map<String, String> metaData = repository.loadMetadataForPackage(
				ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE + ".some_non_existing_package");

		assertNull(metaData);
	}

	@Test
	public void packageAndItsMetadataCanBeSaved() throws Exception {
		createCodeRepository();

		final String packageName = ClassNameUtil.GENERATED_SCRIPTS_ROOT_PACKAGE + ".some_group";
		repository.savePackage(packageName, createGroupMetadata());

		final String savedDir = TestConstants.REPOSITORY_ROOT_DIR_PATH + "/" + packageName.replace('.', '/');
		assertTrue(new File(savedDir).isDirectory());

		String savedMetadata = IOHelper.loadFileContents(savedDir, ClassNameUtil.METADATA_FILE_NAME);

		assertEquals("{\"type\":\"SCRIPT_GROUP\",\"name\":\"Test_Group_001\",\"title\":\"Test Group 001\"}",
				savedMetadata);
	}

	@Test
	public void existingPackageCanNotBeSaved() throws Exception {
		createCodeRepository();

		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage(String.format("The package [%s] already exists",
				ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE));

		repository.savePackage(ClassNameUtil.GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE, null);
	}

	private void createCodeRepository() throws Exception {
		FileUtils.copyDirectory(CODE_REPOSITORY_SOURCE_DIR, REPOSITORY_ROOT_DIR);
	}

	private void saveClassAndSourceAndDiagram() throws Exception {
		saveClass(true);
	}

	private void saveClass() throws Exception {
		saveClass(false);
	}

	private void saveClass(boolean saveSourceAndDiagram) throws Exception {
		String body = IOHelper.loadScript(SCRIPT_SOURCE_FILE_NAME);
		String generatedSource = IOHelper.loadScript(SCRIPT_GENERATED_SOURCE_FILE_NAME);
		byte[] byteCode = CompileHelper.compileScript(SCRIPT_CLASS_NAME, IOUtils.toInputStream(body));
		String diagram = IOHelper.loadScript(SCRIPT_DIAGRAM_FILE_NAME);

		if (saveSourceAndDiagram) {
			repository.save(SCRIPT_CLASS_NAME, byteCode, body, generatedSource, diagram);
		} else {
			repository.save(SCRIPT_CLASS_NAME, byteCode);
		}
	}

	private Map<String, String> createEntityMetadata() {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put("type", Metadata.ENTITY_TYPE);
		metaData.put("name", "Test_Entity_001");
		metaData.put("title", "Test Entity 001");

		return metaData;
	}

	private Map<String, String> createScriptMetadata() {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put("type", Metadata.SCRIPT_TYPE);
		metaData.put("name", "Test_Script_001");
		metaData.put("title", "Test Script 001");

		return metaData;
	}

	private Map<String, String> createGroupMetadata() {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put("type", Metadata.SCRIPT_GROUP_TYPE);
		metaData.put("name", "Test_Group_001");
		metaData.put("title", "Test Group 001");

		return metaData;
	}

}
