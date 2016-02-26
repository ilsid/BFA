package com.ilsid.bfa.action.persistence.filesystem;

import java.io.File;
import java.io.IOException;
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
import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;

public class FilesystemActionRepositoryUnitTest extends BaseUnitTestCase {

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
				"action/default_group/reserve_x20_amount/lib/commons-collections-3.2.1.jar"))));
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
