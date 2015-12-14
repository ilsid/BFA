package com.ilsid.bfa.action.persistence;

import javax.inject.Inject;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionException;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Provides the action implementations.
 * 
 * @author illia.sydorovych
 *
 */
public class ActionLocator {

	private static final String ACTION_DOES_NOT_EXIST_MSG_TPLT = "Action [%s] does not exist";

	private ActionRepository repository;

	/**
	 * Provides the action implementation by the given name.
	 * 
	 * @param actionName
	 *            the action name
	 * @return the proper {@link Action} instance
	 * @throws ActionException
	 *             if the action with the given name does not exist or the repository access issue occurred
	 */
	public Action lookup(String actionName) throws ActionException {
		String actionClassName = getImplementationClassName(actionName);

		if (actionClassName == null) {
			throw new ActionException(String.format(ACTION_DOES_NOT_EXIST_MSG_TPLT, actionName));
		}

		Class<Action> actionClass = loadClass(actionClassName, actionName);
		Action action = createInstance(actionClass, actionName);

		return action;
	}

	/**
	 * Defines the code repository implementation
	 * 
	 * @param repository
	 *            the code repository
	 */
	@Inject
	public void setRepository(ActionRepository repository) {
		this.repository = repository;
	}

	private String getImplementationClassName(String actionName) throws ActionException {
		String actionClassName;
		try {
			actionClassName = repository.getImplementationClassName(actionName);
		} catch (PersistenceException e) {
			throw new ActionException(
					String.format("Failed to load implementation class name for the action [%s]", actionName), e);
		}

		return actionClassName;
	}

	@SuppressWarnings("unchecked")
	private Class<Action> loadClass(String className, String actionName) throws ActionException {
		Class<Action> actionClass;
		try {
			actionClass = (Class<Action>) ActionClassLoader.getLoader(actionName).loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new ActionException(String.format(ACTION_DOES_NOT_EXIST_MSG_TPLT, actionName), e);
		} catch (ClassCastException e) {
			throw new ActionException(
					String.format("Improper implementation class was loaded for the action [%s]", actionName), e);
		}

		return actionClass;
	}

	private Action createInstance(Class<Action> clazz, String actionName) throws ActionException {
		Action instance;
		try {
			instance = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ActionException(String.format("Failed to instantiate the action [%s]", actionName), e);
		}

		return instance;
	}
}
