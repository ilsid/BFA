package com.ilsid.bfa.script;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.DynamicClassLoader;

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
		assertSame(DynamicClassLoader.class, clazz.getClassLoader().getClass());
	}

	@Test
	public void classFromStaticPackageIsLoadedByContextClassLoader() throws Exception {
		String className = "com.ilsid.bfa.test.types.ContractForCustomClassloaderTesting";
		Class<?> clazz = DynamicClassLoader.getInstance().loadClass(className);

		assertEquals(className, clazz.getName());
		assertSame(Thread.currentThread().getContextClassLoader(), clazz.getClassLoader());
	}

	@Test
	public void classFromGeneratedPackageCanBeReloadedByNewLoaderInstance() throws Exception {
		String className = "com.ilsid.bfa.generated.classloadertest.AnotherFooContract";

		DynamicClassLoader initialLoader = DynamicClassLoader.getInstance();
		Class<?> initialClass = initialLoader.loadClass(className);

		DynamicClassLoader.reloadClasses();
		ClassLoader nextLoader = DynamicClassLoader.getInstance();
		Class<?> reloadedClass = nextLoader.loadClass(className);

		assertFalse(initialLoader == nextLoader);
		assertFalse(initialClass == reloadedClass);
	}
	
	@Test
	public void classFromGeneratedPackageCanBeReloadedBySameLoaderInstance() throws Exception {
		String className = "com.ilsid.bfa.generated.classloadertest.YetAnotherFooContract";

		DynamicClassLoader loader = DynamicClassLoader.getInstance();
		Class<?> initialClass = loader.loadClass(className);

		DynamicClassLoader.reloadClasses();
		Class<?> reloadedClass = loader.loadClass(className);

		assertFalse(initialClass == reloadedClass);
	}

	@Test
	public void classFromGeneratedPackageCanBeObtainedAsByteArrayResource() throws Exception {
		String classPath = "com/ilsid/bfa/generated/classloadertest/OneMoreFooContract.class";
		InputStream is = DynamicClassLoader.getInstance().getResourceAsStream(classPath);
		
		assertSame(ByteArrayInputStream.class, is.getClass());
	}
	
	@Test
	public void classFromStaticPackageCanNotBeObtainedAsByteArrayResource() throws Exception {
		String classPath = "com/ilsid/bfa/test/types/ContractForCustomClassloaderTesting.class";
		InputStream is = DynamicClassLoader.getInstance().getResourceAsStream(classPath);
		
		assertFalse(is.getClass() == ByteArrayInputStream.class);
	}

}
