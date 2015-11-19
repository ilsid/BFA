package com.ilsid.bfa.persistence;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import com.ilsid.bfa.BFAError;
import com.ilsid.bfa.script.TypeNameResolver;

/**
 * The class loader for the generated classes. Supports the reloading of the already loaded generated classes. </br>
 * {@link BFAClassLoader#getInstance()} must always be used to avoid runtime exceptions </br>
 * </br>
 * <code>Class<?> clazz = BFAClassLoader.getInstance().loadClass(className);</code> </br>
 * </br>
 * The below code is <b>incorrect</b> </br>
 * </br>
 * <code>
 * BFAClassLoader loader = BFAClassLoader.getInstance();
 * </br>
 * Class<?> clazz = loader.loadClass(className);
 * </code> </br>
 * </br>
 * The latter code may lead to the throwing of {@link IllegalStateException} in case when another thread has been
 * already called {@link BFAClassLoader#reloadClasses()}.
 * 
 * @author illia.sydorovych
 *
 */
public class BFAClassLoader extends ClassLoader {

	private static ConcurrentHashMap<String, Class<?>> cache = new ConcurrentHashMap<>();

	private static BFAClassLoader instance = new BFAClassLoader();

	private static final Object CLASSES_RELOAD_LOCK = new Object();

	private static CodeRepository repository;

	private BFAClassLoader() {
		super(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Returns the actual loader that loads the classes with {@link TypeNameResolver#GENERATED_CLASSES_PACKAGE} package
	 * from the specified code repository. All other classes are loaded by the context class loader of the current
	 * thread. This method returns new loader instance after {@link BFAClassLoader#reloadClasses()} invocation.
	 * 
	 * @return the class loader for the generated classes
	 * @see {@link BFAClassLoader#reloadClasses()}
	 */
	// TODO: Check the performance impact caused by the synchronization block usage. The synchronization is required
	// here because of onClassUpdate() logic.
	public static BFAClassLoader getInstance() {
		synchronized (CLASSES_RELOAD_LOCK) {
			return instance;
		}
	}

	/**
	 * Loads the class with {@link TypeNameResolver#GENERATED_CLASSES_PACKAGE} package from the specified code
	 * repository. All other classes are loaded by the context class loader of the current thread.
	 * 
	 * @param className
	 *            the name of class to load
	 * @return the class with the given name
	 * @throws ClassNotFoundException
	 *             if the class with the given name can't be found
	 * @throws IllegalStateException
	 *             if this method is invoked for the obsolete class loader instance. It may happen if this method was
	 *             called not via {@link BFAClassLoader#getInstance()}, but via the object reference
	 */
	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException, IllegalStateException {
		if (this != instance) {
			throw new IllegalStateException("Obsolete class loader has been invoked");
		}

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
	 * New {@link BFAClassLoader} instance is created. All classes that have been loaded by the old class loader are
	 * reloaded by the new one and are put into the cache. The old classes are removed from the cache.
	 * {@link BFAClassLoader#getInstance()} will return this new instance.
	 */
	public static void reloadClasses() {
		synchronized (CLASSES_RELOAD_LOCK) {
			instance = new BFAClassLoader();

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

}
