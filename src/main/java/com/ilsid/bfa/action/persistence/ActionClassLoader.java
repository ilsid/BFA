package com.ilsid.bfa.action.persistence;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ilsid.bfa.action.Action;
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

	private static ActionRepository repository;

	private static Logger logger;

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

	private void initActionClassesLoader() {
		List<URL> dependencies;
		try {
			dependencies = repository.getDependencies(actionName);
		} catch (PersistenceException e) {
			throw new IllegalStateException(
					String.format("Failed to obtain dependencies for the action [%s]", actionName), e);
		}
		URL[] urls = dependencies.toArray(new URL[dependencies.size()]);
		actionClassesLoader = new SearchURLsFirstClassLoader(urls);
	}

	/*
	 * The loader searches class within URLs first. If a class is not found, the loading is delegated to
	 * DynamicClassLoader.
	 */
	private static class SearchURLsFirstClassLoader extends URLClassLoader {

		public SearchURLsFirstClassLoader(URL[] urls) {
			super(urls, null);
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				return super.findClass(name);
			} catch (ClassNotFoundException e) {
				return DynamicClassLoader.getInstance().loadClass(name);
			}
		}

	}

}
