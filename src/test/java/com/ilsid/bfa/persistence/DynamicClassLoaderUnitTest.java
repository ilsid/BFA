package com.ilsid.bfa.persistence;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;

public class DynamicClassLoaderUnitTest extends BaseUnitTestCase {

	@BeforeClass
	@SuppressWarnings("serial")
	public static void beforeClass() throws Exception {
		ScriptingRepository repository = new com.ilsid.bfa.persistence.filesystem.FilesystemScriptingRepository();
		repository.setConfiguration(new HashMap<String, String>() {
			{
				put("bfa.persistence.fs.root_dir", TestConstants.CODE_REPOSITORY_DIR);
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
	public void nonExistingClassFromGeneratedPackageCanNotBeObtainedAsResource() throws Exception {
		String classPath = "com/ilsid/bfa/generated/classloadertest/NonExisting.class";
		InputStream is = DynamicClassLoader.getInstance().getResourceAsStream(classPath);

		assertNull(is);
	}

	@Test
	public void existingClassFromStaticPackageCanNotBeObtainedAsByteArrayResource() throws Exception {
		String classPath = "com/ilsid/bfa/test/types/ContractForCustomClassloaderTesting.class";
		InputStream is = DynamicClassLoader.getInstance().getResourceAsStream(classPath);

		assertFalse(is.getClass() == ByteArrayInputStream.class);
	}

	@Test
	public void classFromGeneratedPackageCanBeObtainedAsURL() throws Exception {
		String classPath = "com/ilsid/bfa/generated/classloadertest/OneMoreFooContract.class";
		URL url = DynamicClassLoader.getInstance().findResource(classPath);

		assertEquals("byte", url.getProtocol());
		assertEquals("/" + classPath, url.getPath());
	}
	
	@Test
	public void nonExistingClassFromGeneratedPackageCanNotBeObtainedAsURL() throws Exception {
		String classPath = "com/ilsid/bfa/generated/classloadertest/NonExisting.class";
		URL url = DynamicClassLoader.getInstance().findResource(classPath);

		assertNull(url);
	}
	
	@Test
	public void existingClassFromStaticPackageCanNotBeObtainedAsURL() throws Exception {
		String classPath = "com/ilsid/bfa/test/types/ContractForCustomClassloaderTesting.class";
		URL url = DynamicClassLoader.getInstance().findResource(classPath);

		assertNull(url);
	}

}
