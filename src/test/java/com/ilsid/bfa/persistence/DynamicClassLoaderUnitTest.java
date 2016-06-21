package com.ilsid.bfa.persistence;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.ScriptingRepositoryInitializer;

public class DynamicClassLoaderUnitTest extends BaseUnitTestCase {

	@BeforeClass
	public static void beforeClass() throws Exception {
		ScriptingRepositoryInitializer.init();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		ScriptingRepositoryInitializer.cleanup();
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

	@Test
	public void reloadListenerIsTriggeredOnceAfterClassesReloading() {
		ReloadListenerImpl listener = new ReloadListenerImpl("aaa");
		assertFalse(listener.wasInvoked());

		DynamicClassLoader.getInstance().addReloadListener(listener);

		DynamicClassLoader.reloadClasses();
		assertTrue(listener.wasInvoked());

		listener.reset();
		assertFalse(listener.wasInvoked());

		DynamicClassLoader.reloadClasses();
		assertFalse(listener.wasInvoked());
	}

	@Test
	public void uniqueReloadListenersOnlyAreTriggeredAfterClassesReloading() {
		ReloadListenerImpl listener1 = new ReloadListenerImpl("aaa");
		ReloadListenerImpl listener2 = new ReloadListenerImpl("aaa");
		ReloadListenerImpl listener3 = new ReloadListenerImpl("bbb");
		assertEquals(listener1, listener2);
		assertNotEquals(listener1, listener3);
		assertFalse(listener1.wasInvoked());
		assertFalse(listener2.wasInvoked());
		assertFalse(listener3.wasInvoked());

		DynamicClassLoader.getInstance().addReloadListener(listener1);
		DynamicClassLoader.getInstance().addReloadListener(listener2);
		DynamicClassLoader.getInstance().addReloadListener(listener3);

		DynamicClassLoader.reloadClasses();
		assertTrue(listener1.wasInvoked());
		assertFalse(listener2.wasInvoked());
		assertTrue(listener3.wasInvoked());

	}

	private static class ReloadListenerImpl implements DynamicClassLoader.ReloadListener {

		private String name;

		private boolean wasInvoked;

		ReloadListenerImpl(String name) {
			this.name = name;
		}

		public void execute() {
			wasInvoked = true;
		}

		boolean wasInvoked() {
			return wasInvoked;
		}

		void reset() {
			wasInvoked = false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ReloadListenerImpl other = (ReloadListenerImpl) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

	}

}
