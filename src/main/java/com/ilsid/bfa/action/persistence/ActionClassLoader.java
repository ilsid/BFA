package com.ilsid.bfa.action.persistence;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javax.inject.Inject;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * The class loader that is used for the loading of the particular {@link Action} dependencies including the action
 * implementation, action related classes and action third-party classes.
 * 
 * @author illia.sydorovych
 *
 */
// FIXME: class reloading is not implemented
public class ActionClassLoader extends ClassLoader {

	private ClassLoader actionClassesLoader;

	private ActionRepository repository;

	private String actionName;

	/**
	 * Creates an instance for an action with the given name.
	 * 
	 * @param actionName
	 *            action name
	 */
	public ActionClassLoader(String actionName) {
		super(Thread.currentThread().getContextClassLoader());
		this.actionName = actionName;
	}

	/**
	 * Searches the class in the repository first. The class location is defined by the action name. If the class is not
	 * found in the repository, delegates the loading to the context class loader of {@link Thread#currentThread() the
	 * current thread}.
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
		if (actionClassesLoader == null) {
			initActionClassesLoader();
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
	public void setRepository(ActionRepository repository) {
		this.repository = repository;
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
		actionClassesLoader = new ChildFirstURLClassLoader(urls, getParent());
	}

	/*
	 * This loader breaks the parent delegation model. A class is searched within URLs first. If a class is not found,
	 * the loading is delegated to the parent loader.
	 */
	private static class ChildFirstURLClassLoader extends URLClassLoader {

		private ClassLoader parent;

		public ChildFirstURLClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, null);
			this.parent = parent;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				return super.findClass(name);
			} catch (ClassNotFoundException e) {
				return parent.loadClass(name);
			}
		}

	}

}
