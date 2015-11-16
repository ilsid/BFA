package com.ilsid.bfa.persistence;

import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import com.ilsid.bfa.BFAError;
import com.ilsid.bfa.script.TypeNameResolver;

public class BFAClassLoader extends ClassLoader implements ClassUpdateListener {

	private static ConcurrentHashMap<String, Class<?>> cache = new ConcurrentHashMap<>();

	private static BFAClassLoader instance = new BFAClassLoader();

	private static final Object CLASSES_RELOAD_LOCK = new Object();

	private static CodeRepository repository;

	private BFAClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
	}

	// TODO: Check the performance impact caused by the synchronization block usage. The synchronization is required
	// here because of onClassUpdate() logic.
	public static BFAClassLoader getInstance() {
		synchronized (CLASSES_RELOAD_LOCK) {
			return instance;
		}
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		if (className.startsWith(TypeNameResolver.GENERATED_CLASSES_PACKAGE)) {
			Class<?> clazz = cache.get(className);
			if (clazz != null) {
				return clazz;
			}

			byte[] byteCode;
			try {
				byteCode = repository.load(className);
			} catch (PersistenceException e) {
				throw new BFAError(String.format("Failed to load the class [%s] from the repository"), e);
			}

			if (byteCode == null) {
				throw new ClassNotFoundException(String.format("Class [%s] is not found in the repository", className));
			}

			Class<?> alreadyInCacheClass = cache.get(className);
			if (alreadyInCacheClass == null) {
				clazz = defineClass(className, byteCode, 0, byteCode.length);
				resolveClass(clazz);
				// In the worst case, the class with the such name may already has been put into the cache by another
				// thread since the last cache.get(className) invocation. But this is acceptable from the concurrency
				// point. In this case, cache.put() for the same class name and the same class will be just invoked
				// twice
				cache.put(className, clazz);
			} else {
				clazz = alreadyInCacheClass;
			}

			return clazz;
		}

		return super.loadClass(className);
	}

	@Inject
	public static void setRepository(CodeRepository codeRespository) {
		repository = codeRespository;
	}

	/**
	 * New {@link BFAClassLoader} instance is created. All classes that have been loaded by the old class loader are
	 * reloaded by the new one and put into the cache. The old classes are removed from the cache.
	 */
	@Override
	public void onClassUpdate(String className) {
		synchronized (CLASSES_RELOAD_LOCK) {
			instance = new BFAClassLoader();

			ConcurrentHashMap<String, Class<?>> newCache = new ConcurrentHashMap<>();
			for (String ñachedClassName : cache.keySet()) {
				Class<?> classLoadedByNewLoader = null;
				try {
					classLoadedByNewLoader = instance.loadClass(ñachedClassName);
				} catch (ClassNotFoundException e) {
					// It is possible that classes residing in the old cache have been already removed from the
					// repository
					// TODO: log WARN message
				}

				if (classLoadedByNewLoader != null) {
					newCache.put(ñachedClassName, classLoadedByNewLoader);
				}
			}

			cache.clear();
			cache = newCache;
		}

	}
	
}
