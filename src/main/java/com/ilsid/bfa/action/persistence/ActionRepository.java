package com.ilsid.bfa.action.persistence;

import java.net.URL;
import java.util.List;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Provides access to the Actions code repository.
 * 
 * @author illia.sydorovych
 *
 */
// Handle non-default action groups
public interface ActionRepository extends Configurable {

	/**
	 * Loads an implementation class name for the given action.
	 * 
	 * @param actionName
	 *            action name
	 * @return a class name or <code>null</code> if an action with such name does not exist or implementation class for
	 *         this action is not found
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	String getImplementationClassName(String actionName) throws PersistenceException;

	/**
	 * Returns an array of action dependencies. Each URL represents a jar file or classes directory that the given
	 * action depends on.
	 * 
	 * @param actionName
	 *            action name
	 * @return a list of URL's that represent jar files or classes directories. If no dependencies exist than an empty
	 *         list is returned
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	List<URL> getDependencies(String actionName) throws PersistenceException;

}
