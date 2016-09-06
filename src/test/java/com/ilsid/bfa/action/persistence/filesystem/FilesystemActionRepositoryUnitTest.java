package com.ilsid.bfa.action.persistence.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.ActionInfo;
import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;

public class FilesystemActionRepositoryUnitTest extends BaseUnitTestCase {

	private static final File VALID_ACTION_DIR = new File(REPOSITORY_ROOT_DIR,
			"action/default_group/write_x20_system_x20_property");

	private static final String EXISTING_ACTION_GROUP = "Top Level Group 01";

	private static final String EXISTING_ACTION_NAME = "Reserve Amount";

	private static final String NON_EXISTING_ACTION_NAME = "Non Existing Action";

	private ActionRepository repository = new FilesystemActionRepository();

	@Before
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		FileUtils.forceMkdir(REPOSITORY_ROOT_DIR);
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/code_repository/action"),
				new File(REPOSITORY_ROOT_DIR, "action"));

		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", REPOSITORY_ROOT_DIR_PATH);
			}
		});
	}

	@After
	public void tearDown() throws IOException {
		FileUtils.forceDelete(REPOSITORY_ROOT_DIR);
	}

	@Test
	public void implementationClassNameCanBeObtainedForExistingAction() throws Exception {
		assertEquals("com.some.action.impl.SomeAction", repository.getImplementationClassName(EXISTING_ACTION_NAME));
	}

	@Test
	public void noImplementationClassNameCanBeObtainedForNonExisitingAction() throws Exception {
		assertNull(repository.getImplementationClassName(NON_EXISTING_ACTION_NAME));
	}

	@Test
	public void dependencyURLsAreObtainedForExistingAction() throws Exception {
		List<URL> urls = repository.getDependencies(EXISTING_ACTION_NAME);

		assertEquals(3, urls.size());
		assertTrue(urls
				.contains(toURL(new File(REPOSITORY_ROOT_DIR, "action/default_group/reserve_x20_amount/classes/"))));
		assertTrue(urls.contains(toURL(new File(REPOSITORY_ROOT_DIR,
				"action/default_group/reserve_x20_amount/lib/action-dependency-lib.jar"))));
		assertTrue(urls.contains(
				toURL(new File(REPOSITORY_ROOT_DIR, "action/default_group/reserve_x20_amount/lib/mail-1.4.1.jar"))));
	}

	@Test
	public void noDependencyURLsAreObtainedForNonExistingAction() throws Exception {
		assertEquals(0, repository.getDependencies(NON_EXISTING_ACTION_NAME).size());
	}

	@Test
	public void topLevelGroupAndItsMetadataCanBeSaved() throws Exception {
		final String savedDir = REPOSITORY_ROOT_DIR + "/action/test_x20_group_x20_001";
		assertFalse(new File(savedDir).isDirectory());

		repository.createGroup("Test Group 001", createGroupMetadata());

		assertTrue(new File(savedDir).isDirectory());
		assertGroupMetadata(savedDir);
	}

	@Test
	public void childGroupAndItsMetadataCanBeSaved() throws Exception {
		final String savedDir = REPOSITORY_ROOT_DIR + "/action/top_x20_level_x20_group_x20_01/child_x20_group_x20_001";
		assertFalse(new File(savedDir).isDirectory());

		repository.createGroup("Top Level Group 01::Child Group 001", createGroupMetadata());

		assertTrue(new File(savedDir).isDirectory());
		assertGroupMetadata(savedDir);
	}

	@Test
	public void childGroupInNonExistingParentGroupCanNotBeSaved() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage("The action group [Non Existing Group] does not exist");

		repository.createGroup("Non Existing Group::Child Group 001", createGroupMetadata());
	}

	@Test
	public void childGroupInParentGroupWithIncorrectMetadataCanNotBeSaved() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage("The action group [Group with Invalid Metadata] does not exist");

		repository.createGroup("Group with Invalid Metadata::Child Group 001", createGroupMetadata());
	}

	@Test
	public void metadataForPackageCanBeLoaded() throws Exception {
		Map<String, String> metaData = repository.loadMetadataForGroup(EXISTING_ACTION_GROUP);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(EXISTING_ACTION_GROUP, metaData.get(Metadata.NAME));
		assertEquals(EXISTING_ACTION_GROUP, metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForTopLevelPackagesCanBeLoaded() throws Exception {
		List<Map<String, String>> metaDatas = repository.loadMetadataForTopLevelGroups();

		assertEquals(2, metaDatas.size());
		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(Metadata.DEFAULT_GROUP_NAME, metaData.get(Metadata.NAME));
		assertEquals(Metadata.DEFAULT_GROUP_TITLE, metaData.get(Metadata.TITLE));

		metaData = metaDatas.get(1);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(EXISTING_ACTION_GROUP, metaData.get(Metadata.NAME));
		assertEquals(EXISTING_ACTION_GROUP, metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForChildGroupsInExistingGroupCanBeLoaded() throws Exception {
		List<Map<String, String>> metaDatas = repository.loadMetadataForChildGroups(EXISTING_ACTION_GROUP);

		assertEquals(2, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Top Level Group 01::Child Group 01", metaData.get(Metadata.NAME));
		assertEquals("Child Group 01", metaData.get(Metadata.TITLE));

		metaData = metaDatas.get(1);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_GROUP_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Top Level Group 01::Child Group 02", metaData.get(Metadata.NAME));
		assertEquals("Child Group 02", metaData.get(Metadata.TITLE));
	}

	@Test
	public void metadataForChildGroupsInNonExistingGroupCanNotBeLoaded() throws Exception {
		assertEquals(0, repository.loadMetadataForChildGroups("Some Non-Existing Group").size());
	}

	@Test
	public void metadataForActionsInExistingGroupCanBeLoaded() throws Exception {
		List<Map<String, String>> metaDatas = repository.loadMetadataForActions(ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE);

		assertEquals(2, metaDatas.size());

		Map<String, String> metaData = metaDatas.get(0);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Reserve Amount", metaData.get(Metadata.NAME));
		assertEquals("Reserve Amount", metaData.get(Metadata.TITLE));

		metaData = metaDatas.get(1);
		assertEquals(3, metaData.keySet().size());
		assertEquals(Metadata.ACTION_TYPE, metaData.get(Metadata.TYPE));
		assertEquals("Write System Property", metaData.get(Metadata.NAME));
		assertEquals("Write System Property", metaData.get(Metadata.TITLE));

	}

	@Test
	public void metadataForActionsInNonExistingGroupCanNotBeLoaded() throws Exception {
		assertEquals(0, repository.loadMetadataForActions("Some Non-Existing Group").size());
	}

	@Test
	public void validActionPackageCanBeSavedInExistingGroup() throws Exception {
		verifyActionCanBeSaved("Top Level Group 01::New Action 01", "New Action 01",
				"top_x20_level_x20_group_x20_01/new_x20_action_x20_01");
	}

	@Test
	public void validActionPackageCanBeSavedInDefaultGroup() throws Exception {
		verifyActionCanBeSaved("New Action 01", "New Action 01", "default_group/new_x20_action_x20_01");
	}

	@Test
	public void actionPackageCanNotBeSavedInNonExistingGroup() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage("The action group [Non Existing Group] does not exist");

		verifyActionCanBeSaved("Non Existing Group::New Action 01", "New Action 01",
				"non_x20_existing_x20_group/new_x20_action_x20_01");
	}

	@Test
	public void actionPackageCanNotBeSavedIfConfigurationIsMissed() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage("Invalid action package. Configuration is missed");

		final File configFile = new File(VALID_ACTION_DIR, "config.properties");
		final File configFileBackup = new File(VALID_ACTION_DIR, "config_bkp.properties");
		FileUtils.copyFile(configFile, configFileBackup);
		assertTrue(configFile.delete());

		try {
			verifyActionCanBeSaved("New Action 01", "New Action 01", "default_group/new_x20_action_x20_01");
		} finally {
			FileUtils.copyFile(configFileBackup, configFile);
			configFileBackup.delete();
		}
	}

	@Test
	public void actionPackageCanNotBeSavedIfImplementationClassIsMissed() throws Exception {
		exceptionRule.expect(PersistenceException.class);
		exceptionRule.expectMessage(
				"Invalid action package. The implementation class [com.some.action.impl.WriteSystemProperty] is missed");

		final File implClassFile = new File(VALID_ACTION_DIR, "classes/com/some/action/impl/WriteSystemProperty.class");
		final File implClassFileBackup = new File(VALID_ACTION_DIR,
				"classes/com/some/action/impl/WriteSystemProperty_bkp.class");
		FileUtils.copyFile(implClassFile, implClassFileBackup);
		assertTrue(implClassFile.delete());

		try {
			verifyActionCanBeSaved("New Action 01", "New Action 01", "default_group/new_x20_action_x20_01");
		} finally {
			FileUtils.copyFile(implClassFileBackup, implClassFile);
			implClassFileBackup.delete();
		}
	}

	@Test
	public void infoForExistingActionCanBeLoaded() throws Exception {
		ActionInfo info = repository.loadInfo(EXISTING_ACTION_NAME);
		assertEquals("com.some.action.impl.SomeAction", info.getImplementationClassName());
		final List<String> dependencies = info.getDependencies();
		assertEquals(2, dependencies.size());
		assertTrue(dependencies.contains("action-dependency-lib.jar"));
		assertTrue(dependencies.contains("mail-1.4.1.jar"));
	}

	@Test
	public void infoForNonExistingActionCanNotBeLoaded() throws Exception {
		assertNull(repository.loadInfo(NON_EXISTING_ACTION_NAME));
	}

	@Test
	public void existingActionCanBeDeleted() throws Exception {
		final File actionDir = new File(REPOSITORY_ROOT_DIR, "action/default_group/reserve_x20_amount");
		assertTrue(actionDir.isDirectory());
		assertTrue(repository.delete(EXISTING_ACTION_NAME));
		assertFalse(actionDir.exists());
	}

	@Test
	public void nonExistingActionCanNotBeDeleted() throws Exception {
		assertFalse(repository.delete(NON_EXISTING_ACTION_NAME));
	}

	private void verifyActionCanBeSaved(String name, String title, String path) throws Exception {
		// Archives the existing action and saves it with another name
		File actionZipFile = new File(REPOSITORY_ROOT_DIR, "action/default_group/validAction.zip");

		File newActionDir = new File(REPOSITORY_ROOT_DIR, "action/" + path);
		assertFalse(newActionDir.exists());

		IOHelper.zipDirectory(VALID_ACTION_DIR, actionZipFile);
		try (InputStream is = new FileInputStream(actionZipFile)) {
			repository.save(name, is);
		}

		assertTrue(newActionDir.isDirectory());
		IOHelper.assertEqualDirs(VALID_ACTION_DIR, newActionDir);

		File metaFile = new File(REPOSITORY_ROOT_DIR, "action/" + path + "/" + ClassNameUtil.METADATA_FILE_NAME);
		Map<String, String> metaData = IOHelper.toMap(metaFile);

		assertEquals(3, metaData.size());
		assertEquals(Metadata.ACTION_TYPE, metaData.get(Metadata.TYPE));
		assertEquals(name, metaData.get(Metadata.NAME));
		assertEquals(title, metaData.get(Metadata.TITLE));
	}

	private URL toURL(File file) throws Exception {
		return file.toURI().toURL();
	}

	private void assertGroupMetadata(String dir) throws Exception {
		String savedMetadata = IOHelper.loadFileContents(dir, ClassNameUtil.METADATA_FILE_NAME);

		assertEquals("{\"type\":\"ACTION_GROUP\",\"name\":\"Test Group 001\",\"title\":\"Test Group 001\"}",
				savedMetadata);
	}

	private Map<String, String> createGroupMetadata() {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put("type", Metadata.ACTION_GROUP_TYPE);
		metaData.put("name", "Test Group 001");
		metaData.put("title", "Test Group 001");

		return metaData;
	}

}
