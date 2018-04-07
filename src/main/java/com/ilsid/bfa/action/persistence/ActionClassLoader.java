package com.ilsid.bfa.action.persistence;

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.PersistenceLogger;

/**
 * This is the class loader intended for the loading of the particular {@link Action} dependencies including the action
 * implementation, action related classes and action third-party classes. Supports the classes reloading (hot
 * re-deploy).
 * 
 * @author illia.sydorovych
 *
 */
public class ActionClassLoader extends ClassLoader {

	private static Map<String, ActionClassLoader> loaders = new ConcurrentHashMap<>();

	private static ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

	private static ClassLoader commonLibsLoader;

	private static ActionRepository repository;

	private static Logger logger;

	private static final ReadWriteLock COMMON_LIBS_LOCK = new ReentrantReadWriteLock();

	private static final Lock READ_COMMON_LIBS_LOCK = COMMON_LIBS_LOCK.readLock();

	private static final Lock WRITE_COMMON_LIBS_LOCK = COMMON_LIBS_LOCK.writeLock();

	private URLClassLoader actionClassesLoader;

	private String actionName;

	ActionClassLoader(String actionName) {
		super(Thread.currentThread().getContextClassLoader());
		this.actionName = actionName;
	}

	/**
	 * Provides a loader for the action with the given name.
	 * 
	 * @param actionName
	 *            action name
	 * @return a class loader instance
	 */
	public static ClassLoader getLoader(String actionName) {
		synchronized (actionName.intern()) {
			ActionClassLoader loader = loaders.get(actionName);
			if (loader == null) {
				loader = new ActionClassLoader(actionName);
				loaders.put(actionName, loader);
			}

			return loader;
		}
	}

	/**
	 * Reloads classes related to the given action. Implicitly creates new action loader. The consequent invocation of
	 * {@linkplain #getLoader(String)} will return this new loader. Does nothing if the action with the given name has
	 * been never loaded before.
	 * 
	 * @param actionName
	 *            action name
	 * @see #getLoader(String)
	 */
	public static void reload(String actionName) {
		synchronized (actionName.intern()) {
			ActionClassLoader loader = loaders.get(actionName);
			if (loader != null) {
				loader.close();
				loader = new ActionClassLoader(actionName);
				loaders.put(actionName, loader);
			}
		}
	}

	/**
	 * Releases resources of the current commons libraries loader and creates the new one.
	 */
	public static void reloadCommonLibraries() {
		WRITE_COMMON_LIBS_LOCK.lock();
		try {
			releaseCommonLibraries();
			initCommonLibrariesLoader();
		} finally {
			WRITE_COMMON_LIBS_LOCK.unlock();
		}
	}

	/**
	 * Releases resources (like jar files) locked by the given action. The consequent invocation of
	 * {@linkplain #getLoader(String)} will return new loader for this action. Does nothing if the action with the given
	 * name has been never loaded before.
	 * 
	 * @param actionName
	 *            action name
	 * @see #getLoader(String)
	 */
	public static void releaseResources(String actionName) {
		synchronized (actionName.intern()) {
			ActionClassLoader loader = loaders.get(actionName);
			if (loader != null) {
				loaders.remove(actionName);
				loader.close();
			}
		}
	}

	/**
	 * Searches the class in the action repository first. The class location is defined by the action name. If the class
	 * is not found in the repository, the loading is delegated to {@link DynamicClassLoader}.
	 * 
	 * @param className
	 *            the class name
	 * @return the resulting <code>Class</code> object
	 *
	 * @throws ClassNotFoundException
	 *             if the class could not be found
	 */
	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		synchronized (this) {
			if (actionClassesLoader == null) {
				initActionClassesLoader();
			}
		}

		return actionClassesLoader.loadClass(className);
	}

	/**
	 * Defines the code repository implementation.
	 * 
	 * @param repository
	 *            the code repository
	 */
	@Inject
	public static void setRepository(ActionRepository codeRepository) {
		repository = codeRepository;
		initCommonLibrariesLoader();
	}

	/**
	 * Defines the logger implementation.
	 * 
	 * @param loggerImpl
	 *            the logger instance
	 */
	@Inject
	public static void setLogger(@PersistenceLogger Logger loggerImpl) {
		logger = loggerImpl;
	}

	/**
	 * Returns class loader that loads classes from the common libraries storage.
	 * 
	 * @return common libraries class loader
	 */
	public static ClassLoader getCommonLibrariesLoader() {
		READ_COMMON_LIBS_LOCK.lock();
		try {
			return commonLibsLoader;
		} finally {
			READ_COMMON_LIBS_LOCK.unlock();
		}
	}

	void close() {
		if (actionClassesLoader != null) {
			try {
				actionClassesLoader.close();
			} catch (IOException e) {
				if (logger != null) {
					logger.warn(String.format("Failed to close the class loader for the action [%s]", actionName), e);
				} else {
					e.printStackTrace();
				}
			}
		}
	}

	static void releaseCommonLibraries() {
		try {
			((Closeable) commonLibsLoader).close();
		} catch (IOException e) {
			if (logger != null) {
				logger.warn("Failed to close the common libraries loader", e);
			} else {
				e.printStackTrace();
			}
		}
	}

	private static void initCommonLibrariesLoader() {
		List<URL> commonLibs;
		try {
			commonLibs = repository.getCommonLibraries();
		} catch (PersistenceException e) {
			throw new IllegalStateException("Failed to obtain common libraries from repository");
		}

		URL[] urls = commonLibs.toArray(new URL[commonLibs.size()]);
		commonLibsLoader = new URLClassLoader(urls, contextClassLoader);
	}

	private void initActionClassesLoader() {
		List<URL> dependencies = new LinkedList<>();
		try {
			dependencies.addAll(repository.getDependencies(actionName));
		} catch (PersistenceException e) {
			throw new IllegalStateException(
					String.format("Failed to obtain dependencies for the action [%s]", actionName), e);
		}
		URL[] urls = dependencies.toArray(new URL[dependencies.size()]);
		actionClassesLoader = new DependenciesFirstClassLoader(urls, actionName);
	}

	/*
	 * The loader searches a class within the provided URLs first and then in the common libraries storage. If the class
	 * is not found, the loading is delegated to DynamicClassLoader.
	 */
	private static class DependenciesFirstClassLoader extends URLClassLoader {

		private String actionName;

		public DependenciesFirstClassLoader(URL[] urls, String actionName) {
			super(urls, null);
			this.actionName = actionName;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				return super.findClass(name);
			} catch (ClassNotFoundException e) {

				try {
					return commonLibsLoader.loadClass(name);
				} catch (ClassNotFoundException e2) {

					if (name.startsWith(ClassNameUtil.GENERATED_CLASSES_PACKAGE)) {
						// Reload listener is registered in case of generated class, because this class may be reloaded
						// in future. This is needed because generated classes used in actions must be loaded by the
						// actual (most recent) dynamic class loader instance to avoid ClassCastException issues
						DynamicClassLoader.addReloadListener(new ReloadActionListener(actionName));
					}

					return DynamicClassLoader.getInstance().loadClass(name);
				}

			}
		}

	}

	private static class ReloadActionListener implements DynamicClassLoader.ReloadListener {

		private String actionName;

		ReloadActionListener(String actionName) {
			this.actionName = actionName;
		}

		public void execute() {
			ActionClassLoader.reload(actionName);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((actionName == null) ? 0 : actionName.hashCode());
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
			ReloadActionListener other = (ReloadActionListener) obj;
			if (actionName == null) {
				if (other.actionName != null)
					return false;
			} else if (!actionName.equals(other.actionName))
				return false;
			return true;
		}
	}

}
