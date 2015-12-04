package com.ilsid.bfa.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import com.ilsid.bfa.BFAError;
import com.ilsid.bfa.common.ClassNameUtil;

/**
 * The class loader for the generated classes. Supports the reloading of the already loaded generated classes. </br>
 * {@link DynamicClassLoader#getInstance()} must always be used to avoid runtime exceptions </br>
 * </br>
 * <code>Class<?> clazz = DynamicClassLoader.getInstance().loadClass(className);</code> </br>
 * </br>
 * The below code is <b>incorrect</b> </br>
 * </br>
 * <code>
 * DynamicClassLoader loader = DynamicClassLoader.getInstance();
 * </br>
 * Class<?> clazz = loader.loadClass(className);
 * </code> </br>
 * </br>
 * The latter code may lead to the throwing of {@link IllegalStateException} in case when another thread has been
 * already called {@link DynamicClassLoader#reloadClasses()}.
 * 
 * @author illia.sydorovych
 *
 */
public class DynamicClassLoader extends ClassLoader {

	private static final String URL_PREFIX = "byte:///";

	private static final String CLASS_FILE_EXTENSION = ".class";

	private static final String GENERATED_CLASSES_DIR = ClassNameUtil.GENERATED_CLASSES_PACKAGE.replace('.', '/');

	private static ConcurrentHashMap<String, Class<?>> cache = new ConcurrentHashMap<>();

	private static DynamicClassLoader instance = new DynamicClassLoader();

	private static final Object CLASSES_RELOAD_LOCK = new Object();

	private static CodeRepository repository;

	private DynamicClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Returns the actual loader that loads the classes with {@link TypeNameResolver#GENERATED_CLASSES_PACKAGE} package
	 * from the specified code repository. All other classes are loaded by the context class loader of the current
	 * thread. This method returns new loader instance after {@link DynamicClassLoader#reloadClasses()} invocation.
	 * 
	 * @return the class loader for the generated classes
	 * @see {@link DynamicClassLoader#reloadClasses()}
	 */
	// TODO: Check the performance impact caused by the synchronization block usage. The synchronization is required
	// here because of reloadClasses() logic.
	public static DynamicClassLoader getInstance() {
		synchronized (CLASSES_RELOAD_LOCK) {
			return instance;
		}
	}

	/**
	 * Provides URL for a generated class.
	 * 
	 * @return URL for a resource name that represents a generated class (belonging to
	 *         {@link TypeNameResolver#GENERATED_CLASSES_PACKAGE} package) or <code>null</code> otherwise
	 */
	@Override
	protected URL findResource(String name) {
		if (name.startsWith(GENERATED_CLASSES_DIR) && name.endsWith(CLASS_FILE_EXTENSION)) {
			String urlSpec = URL_PREFIX + name;
			try {
				return new URL(null, urlSpec, new ByteArrayHandler());
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("The URL is malformed: " + urlSpec, e);
			}
		}

		return null;
	};

	/**
	 * Loads the class with {@link TypeNameResolver#GENERATED_CLASSES_PACKAGE} package from the specified code
	 * repository. If the class has been already loaded, it is got from the loader's cache. All other classes are loaded
	 * by the context class loader of the current thread.
	 * 
	 * @param className
	 *            the name of class to load
	 * @return the class with the given name
	 * @throws ClassNotFoundException
	 *             if the class with the given name can't be found
	 * @throws IllegalStateException
	 *             if this method is invoked for the obsolete class loader instance. It may happen if this method was
	 *             called not via {@link DynamicClassLoader#getInstance()}, but via the object reference
	 */
	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException, IllegalStateException {
		validateInstance();

		if (className.startsWith(ClassNameUtil.GENERATED_CLASSES_PACKAGE)) {
			Class<?> clazz = cache.get(className);
			if (clazz != null) {
				return clazz;
			}

			byte[] byteCode = loadClassByteCode(className);

			Class<?> alreadyInCacheClass = cache.get(className);
			if (alreadyInCacheClass == null) {
				clazz = defineClass(className, byteCode, 0, byteCode.length);
				resolveClass(clazz);
				// In the worst case, the class with the such name may already has been put into the cache by another
				// thread since the last cache.get(className) invocation. But this is acceptable from the concurrency
				// point. In this case, cache.put() for the same class will be just invoked twice.
				cache.put(className, clazz);
			} else {
				clazz = alreadyInCacheClass;
			}

			return clazz;
		}

		return super.loadClass(className);
	}

	/**
	 * Loads the byte code for classes with {@link TypeNameResolver#GENERATED_CLASSES_PACKAGE} package from the
	 * specified code repository. Return <code>null</code> for all other classes.
	 * 
	 * @param className
	 *            the class name
	 * @return the byte code for the generated classes or <code>null</code> for all other classes
	 * @throws ClassNotFoundException
	 *             if the generated class with the given named does not exist in the repository
	 * @throws IllegalStateException
	 *             if this method is invoked for the obsolete class loader instance. It may happen if this method was
	 *             called not via {@link DynamicClassLoader#getInstance()}, but via the object reference
	 */
	public byte[] loadByteCode(String className) throws ClassNotFoundException, IllegalStateException {
		validateInstance();

		if (className.startsWith(ClassNameUtil.GENERATED_CLASSES_PACKAGE)) {
			return loadClassByteCode(className);
		} else {
			return null;
		}
	}

	/**
	 * New {@link DynamicClassLoader} instance is created. All classes that have been loaded by the old class loader are
	 * reloaded by the new one and are put into the cache. The old classes are removed from the cache.
	 * {@link DynamicClassLoader#getInstance()} will return this new instance.
	 */
	public static void reloadClasses() {
		synchronized (CLASSES_RELOAD_LOCK) {
			instance = new DynamicClassLoader();

			// Force reloading of all cached classes
			Set<String> cachedClassNames = new HashSet<>(cache.keySet());
			cache.clear();
			for (String className : cachedClassNames) {
				try {
					instance.loadClass(className);
				} catch (ClassNotFoundException e) {
					// It is possible that classes residing in the old cache have been already removed from the
					// repository
					// TODO: log WARN message
				}
			}
		}
	}

	/**
	 * Defines the code repository implementation.
	 * 
	 * @param codeRespository
	 *            the code repository
	 */
	@Inject
	public static void setRepository(CodeRepository codeRespository) {
		repository = codeRespository;
	}

	private void validateInstance() throws IllegalStateException {
		if (this != instance) {
			throw new IllegalStateException("Obsolete class loader has been invoked");
		}
	}

	private static byte[] loadClassByteCode(String className) throws ClassNotFoundException {
		byte[] byteCode;
		try {
			byteCode = repository.load(className);
		} catch (PersistenceException e) {
			throw new BFAError(String.format("Failed to load the class [%s] from the repository"), e);
		}

		if (byteCode == null) {
			throw new ClassNotFoundException(String.format("Class [%s] is not found in the repository", className));
		}

		return byteCode;
	}

	private class ByteArrayHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new ByteArrayUrlConnection(u);
		}
	}

	private class ByteArrayUrlConnection extends URLConnection {
		public ByteArrayUrlConnection(URL url) {
			super(url);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			String path = url.getPath();
			final String className = path.substring(1, path.length() - CLASS_FILE_EXTENSION.length()).replace('/', '.');
			try {
				return new ByteArrayInputStream(loadClassByteCode(className));
			} catch (ClassNotFoundException e) {
				throw new IOException(String.format("Failed to load a byte code for the class [%s]", className), e);
			}
		}
	}

}
