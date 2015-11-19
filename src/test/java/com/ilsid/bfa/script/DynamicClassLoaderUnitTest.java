package com.ilsid.bfa.script;

import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.CodeRepository;

public class DynamicClassLoaderUnitTest extends BaseUnitTestCase {

	@BeforeClass
	@SuppressWarnings("serial")
	public static void beforeClass() throws Exception {
		CodeRepository repository = new com.ilsid.bfa.persistence.filesystem.FSCodeRepository();
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.TEST_RESOURCES_DIR + "/code_repository");
			}
		});

		DynamicClassLoader.setRepository(repository);
	}

	@Test
	public void classFromGeneratedPackageIsLoadedByCustomClassLoader() throws Exception {
		String className = "com.ilsid.bfa.generated.classloadertest.FooContract";
		Class<?> clazz = DynamicClassLoader.getInstance().loadClass(className);

		assertEquals(className, clazz.getName());
		assertEquals(DynamicClassLoader.class, clazz.getClassLoader().getClass());
	}

	@Test
	public void classFromStaticPackageIsLoadedByContextClassLoader() throws Exception {
		String className = "com.ilsid.bfa.test.types.ContractForCustomClassloaderTesting";
		Class<?> clazz = DynamicClassLoader.getInstance().loadClass(className);

		assertEquals(className, clazz.getName());
		assertSame(Thread.currentThread().getContextClassLoader(), clazz.getClassLoader());
	}

	@Test
	public void classFromGeneratedPackageCanBeReloaded() throws Exception {
		String className = "com.ilsid.bfa.generated.classloadertest.AnotherFooContract";

		DynamicClassLoader intialLoader = DynamicClassLoader.getInstance();
		Class<?> initialClass = intialLoader.loadClass(className);

		DynamicClassLoader.reloadClasses();
		ClassLoader nextLoader = DynamicClassLoader.getInstance();
		Class<?> reloadedClass = nextLoader.loadClass(className);

		assertFalse(intialLoader == nextLoader);
		assertFalse(initialClass == reloadedClass);
	}

	@Test
	public void referenceToObsoleteLoaderIsProhibited() throws Exception {
		exceptionRule.expect(IllegalStateException.class);
		exceptionRule.expectMessage("Obsolete class loader has been invoked");

		String className = "com.ilsid.bfa.generated.classloadertest.YetAnotherFooContract";

		DynamicClassLoader intialInstance = DynamicClassLoader.getInstance();
		intialInstance.loadClass(className);
		DynamicClassLoader.reloadClasses();
		// the reference to the initial instance becomes obsolete after the classes reloading
		intialInstance.loadClass(className);
	}

}