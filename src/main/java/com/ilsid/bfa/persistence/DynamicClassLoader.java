package com.ilsid.bfa.persistence;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import com.ilsid.bfa.BFAError;
import com.ilsid.bfa.common.ClassNameUtil;

/**
 * The class loader for the generated classes. Supports the reloading of the already loaded generated classes.
 * 
 * @author illia.sydorovych
 *
 */
public class DynamicClassLoader extends ClassLoader {

	private static final String URL_PREFIX = "byte:///";

	private static final String GENERATED_CLASSES_DIR = ClassNameUtil.GENERATED_CLASSES_PACKAGE.replace('.', '/');

	private static Map<String, Class<?>> cache = new ConcurrentHashMap<>();

	private static Set<ReloadListener> reloadListeners = Collections
			.newSetFromMap(new ConcurrentHashMap<ReloadListener, Boolean>());

	private static Set<ReloadListener> permanentReloadListeners = Collections
			.newSetFromMap(new ConcurrentHashMap<ReloadListener, Boolean>());

	private static DynamicClassLoader instance = new DynamicClassLoader();

	private static final ReadWriteLock RELOAD_LOCK = new ReentrantReadWriteLock();

	private static final Lock READ_RELOAD_LOCK = RELOAD_LOCK.readLock();

	private static final Lock WRITE_RELOAD_LOCK = RELOAD_LOCK.writeLock();

	private static ScriptingRepository repository;

	private DynamicClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Returns the actual loader that loads the classes with {@link ClassNameUtil#GENERATED_CLASSES_PACKAGE} package
	 * from the specified code repository. All other classes are loaded by the context class loader of the current
	 * thread. This method returns new loader instance after {@link DynamicClassLoader#reloadClasses()} invocation.
	 * 
	 * @return the class loader for the generated classes
	 * @see {@link DynamicClassLoader#reloadClasses()}
	 */
	public static DynamicClassLoader getInstance() {
		READ_RELOAD_LOCK.lock();
		try {
			return instance;
		} finally {
			READ_RELOAD_LOCK.unlock();
		}
	}

	/**
	 * Provides URL for a generated class.
	 * 
	 * @return URL for a resource name that represents a generated class (belonging to
	 *         {@link ClassNameUtil#GENERATED_CLASSES_PACKAGE} package) or <code>null</code> otherwise
	 */
	@Override
	protected URL findResource(String name) {
		READ_RELOAD_LOCK.lock();
		try {
			return instance.doFindResource(name);
		} finally {
			READ_RELOAD_LOCK.unlock();
		}
	};

	/**
	 * Loads the class with {@link ClassNameUtil#GENERATED_CLASSES_PACKAGE} package from the specified code repository.
	 * If the class has been already loaded, it is got from the loader's cache. All other classes are loaded by the
	 * context class loader of the current thread.
	 * 
	 * @param className
	 *            the name of class to load
	 * @return the class with the given name
	 * @throws ClassNotFoundException
	 *             if the class with the given name can't be found
	 */
	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException, IllegalStateException {
		READ_RELOAD_LOCK.lock();
		try {
			return instance.doLoadClass(className);
		} finally {
			READ_RELOAD_LOCK.unlock();
		}
	}

	/**
	 * New {@link DynamicClassLoader} instance is created. All classes that have been loaded by the old class loader are
	 * reloaded by the new one and are put into the cache. The old classes are removed from the cache.
	 * {@link DynamicClassLoader#getInstance()} will return this new instance. All registered reloading listeners are
	 * triggered.
	 * 
	 * @see #addReloadListener(ReloadListener)
	 */
	public static void reloadClasses() {
		WRITE_RELOAD_LOCK.lock();
		try {
			doReload();
		} finally {
			WRITE_RELOAD_LOCK.unlock();
		}
	}

	/**
	 * Registers a listener that is triggered right after classes reloading. A listener is triggered only once and is
	 * unregistered after the reloading. There is no effect, if such listener has been already registered (according to
	 * {@linkplain Set#add(Object)} contract).
	 * 
	 * @param listener
	 *            reloading listener
	 * @see #reloadClasses()
	 */
	public static void addReloadListener(ReloadListener listener) {
		READ_RELOAD_LOCK.lock();
		try {
			reloadListeners.add(listener);
		} finally {
			READ_RELOAD_LOCK.unlock();
		}
	}

	/**
	 * Registers a listener that is triggered right after classes reloading. A listener is permanent and is triggered
	 * each time the reloading occurs. There is no effect, if such listener has been already registered (according to
	 * {@linkplain Set#add(Object)} contract).
	 * 
	 * @param listener
	 *            reloading listener
	 * @see #reloadClasses()
	 */
	public static void addPermanentReloadListener(ReloadListener listener) {
		READ_RELOAD_LOCK.lock();
		try {
			permanentReloadListeners.add(listener);
		} finally {
			READ_RELOAD_LOCK.unlock();
		}
	}

	/**
	 * Defines the code repository implementation.
	 * 
	 * @param codeRepository
	 *            the code repository
	 */
	@Inject
	public static void setRepository(ScriptingRepository codeRepository) {
		repository = codeRepository;
	}

	/**
	 * Listener that is triggered right after classes reloading.
	 * 
	 * @author illia.sydorovych
	 *
	 */
	public interface ReloadListener {

		void execute();

	}

	private static void doReload() {
		instance = new DynamicClassLoader();

		// Force reloading of all cached classes
		Set<String> cachedClassNames = new HashSet<>(cache.keySet());
		cache.clear();
		cache = new ConcurrentHashMap<>();

		for (String className : cachedClassNames) {
			try {
				instance.loadClass(className);
			} catch (ClassNotFoundException e) {
				// It is possible that classes residing in the old cache have been already removed from the
				// repository
				// TODO: log WARN message
			}
		}

		for (ReloadListener listener : permanentReloadListeners) {
			listener.execute();
		}

		for (ReloadListener listener : reloadListeners) {
			listener.execute();
		}
		reloadListeners.clear();
		reloadListeners = Collections.newSetFromMap(new ConcurrentHashMap<ReloadListener, Boolean>());

		Set<ReloadListener> tmpPermanentReloadListeners = Collections
				.newSetFromMap(new ConcurrentHashMap<ReloadListener, Boolean>());
		tmpPermanentReloadListeners.addAll(permanentReloadListeners);
		permanentReloadListeners.clear();
		permanentReloadListeners = tmpPermanentReloadListeners;
	}

	private URL doFindResource(String name) {
		if (name.startsWith(GENERATED_CLASSES_DIR) && name.endsWith(ClassNameUtil.CLASS_FILE_EXTENSION)
				&& classExists(name)) {
			String urlSpec = URL_PREFIX + name;
			try {
				return new URL(null, urlSpec, new ByteArrayHandler());
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("The URL is malformed: " + urlSpec, e);
			}
		}

		return null;
	};

	private boolean classExists(String path) {
		String className = path.substring(0, path.length() - ClassNameUtil.CLASS_FILE_EXTENSION.length()).replace('/',
				'.');
		byte[] byteCode;
		try {
			byteCode = repository.load(className);
		} catch (PersistenceException e) {
			throw new BFAError(String.format("Failed to load the class [%s] from the repository", className), e);
		}

		return byteCode != null;
	}

	private Class<?> doLoadClass(String className) throws ClassNotFoundException, IllegalStateException {
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

	private static byte[] loadClassByteCode(String className) throws ClassNotFoundException {
		byte[] byteCode;
		try {
			byteCode = repository.load(className);
		} catch (PersistenceException e) {
			throw new BFAError(String.format("Failed to load the class [%s] from the repository", className), e);
		}

		if (byteCode == null) {
			throw new ClassNotFoundException(String.format("Class [%s] is not found in the repository", className));
		}

		return byteCode;
	}

	private static class ByteArrayHandler extends URLStreamHandler {
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new ByteArrayUrlConnection(u);
		}
	}

	private static class ByteArrayUrlConnection extends URLConnection {
		public ByteArrayUrlConnection(URL url) {
			super(url);
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			String path = url.getPath();
			final String className = path.substring(1, path.length() - ClassNameUtil.CLASS_FILE_EXTENSION.length())
					.replace('/', '.');
			try {
				return new ByteArrayInputStream(loadClassByteCode(className));
			} catch (ClassNotFoundException e) {
				throw new IOException(String.format("Failed to load a byte code for the class [%s]", className), e);
			}
		}
	}

}
