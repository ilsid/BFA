package com.ilsid.bfa.action.persistence;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.persistence.filesystem.FilesystemActionRepository;

public class ActionClassLoaderUnitTest extends BaseUnitTestCase {

	private static final String CLASSLOADER_CLASS_NAME = "com.ilsid.bfa.action.persistence.ActionClassLoader$ChildFirstURLClassLoader";

	private static final String TEST_ACTION_RESULT = "Test Action Result";

	private static final String ACTION_IMPL_CLASS_NAME = "com.some.action.impl.SomeAction";

	private static final String ACTION_NAME = "Reserve Amount";

	private ActionRepository repository = new FilesystemActionRepository();

	private ActionClassLoader loader;

	@Before
	@SuppressWarnings("serial")
	public void setUp() throws Exception {
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.CODE_REPOSITORY_DIR);
			}
		});

		loader = new ActionClassLoader(ACTION_NAME);
		loader.setRepository(repository);
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
		final String className = "org.apache.commons.collections.CollectionUtils";
		makeSureClassIsNotInClasspathOfCurrentLoader(className);

		Class<?> clazz = loader.loadClass(className);
		assertEquals(CLASSLOADER_CLASS_NAME, clazz.getClassLoader().getClass().getName());

		Field field = clazz.getDeclaredField("EMPTY_COLLECTION");
		assertEquals(0, ((Collection<?>) field.get(null)).size());
	}

	@Test
	public void actionLocalDependencyCanBeLoaded() throws Exception {
		final String className = "com.some.action.impl.ActionResultProvider";
		makeSureClassIsNotInClasspathOfCurrentLoader(className);

		Class<?> clazz = loader.loadClass(className);
		assertEquals(CLASSLOADER_CLASS_NAME, clazz.getClassLoader().getClass().getName());

		Object obj = clazz.newInstance();
		Method method = clazz.getDeclaredMethod("getResult");
		assertEquals(TEST_ACTION_RESULT, method.invoke(obj));
	}

	@Test
	public void classCanNotBeFoundForNonExistingAction() throws Exception {
		exceptionRule.expect(ClassNotFoundException.class);

		loader = new ActionClassLoader("some.non.existing.ActionImpl");
		loader.setRepository(repository);

		loader.loadClass(ACTION_IMPL_CLASS_NAME);
	}

	private void makeSureClassIsNotInClasspathOfCurrentLoader(String className) {
		try {
			Class.forName(className);
			fail(ClassNotFoundException.class.getName() + " exception was expected");
		} catch (ClassNotFoundException e) {
		}
	}
}
