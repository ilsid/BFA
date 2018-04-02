package com.ilsid.bfa.action.persistence;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.persistence.filesystem.ActionRepositoryInitializer;
import com.ilsid.bfa.script.ScriptingRepositoryInitializer;

public class ActionClassLoaderUnitTest extends BaseUnitTestCase {

	private static final String TMP_CODE_REPOSITORY_PATH = TestConstants.TEST_RESOURCES_DIR
			+ "/__tmp_action_repository";

	private static final File TMP_CODE_REPOSITORY_DIR = new File(TMP_CODE_REPOSITORY_PATH);

	private static final File TMP_ACTION_CODE_REPOSITORY_DIR = new File(TMP_CODE_REPOSITORY_PATH + "/action");

	private static final String LOADER_CLASS_NAME = "com.ilsid.bfa.action.persistence.ActionClassLoader$SearchURLsFirstClassLoader";

	private static final String TEST_ACTION_RESULT = "Test Action Result";

	private static final String ACTION_IMPL_CLASS_NAME = "com.some.action.impl.SomeAction";

	private static final String ACTION_NAME = "Reserve Amount";

	private static final String ENTITY_CLASS_NAME = "com.ilsid.bfa.generated.entity.default_group.Contract";

	private ClassLoader loader;

	@BeforeClass
	public static void beforeClass() throws Exception {
		final File commonLibDir = new File(TMP_CODE_REPOSITORY_DIR, ActionRepositoryInitializer.COMMON_LIB_DIR);

		FileUtils.forceMkdir(TMP_CODE_REPOSITORY_DIR);
		FileUtils.copyDirectory(new File(TestConstants.TEST_RESOURCES_DIR + "/common_lib"), commonLibDir);

		ActionClassLoader.setRepository(ActionRepositoryInitializer.init(TMP_CODE_REPOSITORY_DIR.getAbsolutePath()));
		ScriptingRepositoryInitializer.init(commonLibDir.getAbsolutePath());
	}

	@AfterClass
	public static void afterClass() throws Exception {
		FileUtils.forceDelete(TMP_CODE_REPOSITORY_DIR);
		ScriptingRepositoryInitializer.cleanup();
	}

	@Before
	public void setUp() throws Exception {
		FileUtils.copyDirectory(new File(TestConstants.CODE_REPOSITORY_DIR + "/action"),
				TMP_ACTION_CODE_REPOSITORY_DIR);

		loader = new ActionClassLoader(ACTION_NAME);
	}

	@After
	public void tearDown() throws Exception {
		((ActionClassLoader) loader).close();
	}

	@Test
	public void actionWithLocalAndThirdPartyDependenciesCanBeLoaded() throws Exception {
		@SuppressWarnings("unchecked")
		Class<Action> clazz = (Class<Action>) loader.loadClass(ACTION_IMPL_CLASS_NAME);

		assertEquals(ACTION_IMPL_CLASS_NAME, clazz.getName());
		Action action = clazz.newInstance();
		assertEquals(TEST_ACTION_RESULT, action.execute()[0]);
	}

	@Test
	public void actionThirdPartyDependencyCanBeLoaded() throws Exception {
		final String className = "com.ilsid.bfa.SomeClassWithinActionLib";
		makeSureClassIsNotInClasspathOfCurrentLoader(className);

		Class<?> clazz = loader.loadClass(className);
		assertEquals(LOADER_CLASS_NAME, clazz.getClassLoader().getClass().getName());

		Field field = clazz.getDeclaredField("SOME_CONSTANT");
		assertEquals("Some Value", field.get(null));
	}

	@Test
	public void commonLibDependencyCanBeLoaded() throws Exception {
		final String className = "org.jboss.logging.BasicLogger";
		makeSureClassIsNotInClasspathOfCurrentLoader(className);

		Class<?> clazz = loader.loadClass(className);
		assertEquals(LOADER_CLASS_NAME, clazz.getClassLoader().getClass().getName());
	}

	@Test
	public void actionLocalDependencyCanBeLoaded() throws Exception {
		final String className = "com.some.action.impl.ActionResultProvider";
		makeSureClassIsNotInClasspathOfCurrentLoader(className);

		Class<?> clazz = loader.loadClass(className);
		assertEquals(LOADER_CLASS_NAME, clazz.getClassLoader().getClass().getName());

		Object obj = clazz.newInstance();
		Method method = clazz.getDeclaredMethod("getResult");
		assertEquals(TEST_ACTION_RESULT, method.invoke(obj));
	}

	@Test
	public void classCanNotBeFoundForNonExistingAction() throws Exception {
		exceptionRule.expect(ClassNotFoundException.class);

		loader = new ActionClassLoader("Non Existing Action");
		loader.loadClass(ACTION_IMPL_CLASS_NAME);
	}

	@Test
	public void actionClassCanBeReloaded() throws Exception {
		@SuppressWarnings("unchecked")
		Class<Action> clazz = (Class<Action>) loader.loadClass(ACTION_IMPL_CLASS_NAME);

		assertEquals(ACTION_IMPL_CLASS_NAME, clazz.getName());
		Action action = clazz.newInstance();
		assertEquals(TEST_ACTION_RESULT, action.execute()[0]);

		// Update the action class in the repository
		FileUtils.copyFileToDirectory(
				new File(TestConstants.CODE_REPOSITORY_DIR + "/updated_action/com/some/action/impl/SomeAction.class"),
				new File(TMP_CODE_REPOSITORY_PATH
						+ "/action/default_group/reserve_x20_amount/classes/com/some/action/impl"));

		// The current loader must be closed explicitly as it was created via package private constructor (but not via
		// getLoader() static method)
		((ActionClassLoader) loader).close();
		ActionClassLoader.reload(ACTION_NAME);
		loader = ActionClassLoader.getLoader(ACTION_NAME);

		@SuppressWarnings("unchecked")
		Class<Action> reloadedClazz = (Class<Action>) loader.loadClass(ACTION_IMPL_CLASS_NAME);

		assertTrue(reloadedClazz != clazz);
		assertEquals(ACTION_IMPL_CLASS_NAME, reloadedClazz.getName());

		Action reloadedAction = reloadedClazz.newInstance();
		String newActionResult = TEST_ACTION_RESULT + " Updated";
		assertEquals(newActionResult, reloadedAction.execute()[0]);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void actionClassIsLoadedOnceBySameLoader() throws Exception {
		Class<Action> clazz1 = (Class<Action>) loader.loadClass(ACTION_IMPL_CLASS_NAME);
		Class<Action> clazz2 = (Class<Action>) loader.loadClass(ACTION_IMPL_CLASS_NAME);

		assertSame(clazz1, clazz2);
	}

	@Test
	public void generatedClassCanBeLoadedFromScriptingRepository() throws Exception {
		Class<?> clazz = loader.loadClass(ENTITY_CLASS_NAME);
		assertEquals(ENTITY_CLASS_NAME, clazz.getName());
	}

	private void makeSureClassIsNotInClasspathOfCurrentLoader(String className) {
		try {
			Class.forName(className);
			fail(ClassNotFoundException.class.getName() + " exception was expected");
		} catch (ClassNotFoundException e) {
		}
	}
}
