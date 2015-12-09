package com.ilsid.bfa.action.persistence.filesystem;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.persistence.ActionRepository;

public class FilesystemActionRepositoryUnitTest extends BaseUnitTestCase {

	private final static String ROOT_DIR_PATH = TestConstants.TEST_RESOURCES_DIR + "/code_repository";
	
	private static final String EXISTING_ACTION_NAME = "Reserve Amount";

	private static final String NON_EXISTING_ACTION_NAME = "Non Existing Action";

	private ActionRepository repository = new FilesystemActionRepository();

	@Before
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", ROOT_DIR_PATH);
			}
		});
	}

	@Test
	public void implementationClassNameIsObtainedForExistingAction() throws Exception {
		assertEquals("com.some.action.impl.SomeAction", repository.getImplementationClassName(EXISTING_ACTION_NAME));
	}

	@Test
	public void noImplementationClassNameIsObtainedForNonExisitingAction() throws Exception {
		assertNull(repository.getImplementationClassName(NON_EXISTING_ACTION_NAME));
	}

	@Test
	public void dependencyURLsAreObtainedForExistingAction() throws Exception {
		List<URL> urls = repository.getDependencies(EXISTING_ACTION_NAME);

		assertEquals(3, urls.size());
		assertTrue(urls.contains(toURL(new File(ROOT_DIR_PATH + "/action/default_group/Reserve_x20_Amount/classes/"))));
		assertTrue(urls.contains(toURL(new File(
				ROOT_DIR_PATH + "/action/default_group/Reserve_x20_Amount/lib/commons-collections-3.2.1.jar"))));
		assertTrue(urls.contains(
				toURL(new File(ROOT_DIR_PATH + "/action/default_group/Reserve_x20_Amount/lib/mail-1.4.1.jar"))));
	}

	@Test
	public void noDependencyURLsAreObtainedForNonExistingAction() throws Exception {
		assertEquals(0, repository.getDependencies(NON_EXISTING_ACTION_NAME).size());
	}

	private URL toURL(File file) throws Exception {
		return file.toURI().toURL();
	}

}
