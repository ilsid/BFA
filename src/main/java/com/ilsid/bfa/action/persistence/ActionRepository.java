package com.ilsid.bfa.action.persistence;

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
	 * Loads a byte code for a given action.
	 * 
	 * @param actionName
	 *            action name
	 * @return a byte array representing Java byte code or <code>null</code>, if an action with such name does not exist
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 * 
	 */
	byte[] load(String actionName) throws PersistenceException;

}
