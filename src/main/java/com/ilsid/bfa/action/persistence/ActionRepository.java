package com.ilsid.bfa.action.persistence;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Provides access to the Actions code repository.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: Handle non-default action groups
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
	 * Returns a list of action dependencies. Each URL represents a jar file or classes directory that the given action
	 * depends on.
	 * 
	 * @param actionName
	 *            action name
	 * @return a list of URL's that represent jar files or classes directories. If no dependencies exist than an empty
	 *         list is returned
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	List<URL> getDependencies(String actionName) throws PersistenceException;

	/**
	 * Creates new action group.
	 * 
	 * @param groupName
	 *            group name
	 * @param metaData
	 *            group meta-data
	 * @throws PersistenceException
	 *             <ul>
	 *             <li>if the group with the given name already exists in the repository</li>
	 *             <li>if the group is not a top-level and its parent group does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	void createGroup(String groupName, Map<String, String> metaData) throws PersistenceException;

	/**
	 * Loads meta-data for the action group.
	 * 
	 * @param groupName
	 *            group name
	 * @return the meta-data for the given group or <code>null</code> if such group does not exist or has no
	 *         corresponding meta-data
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	Map<String, String> loadMetadataForGroup(String groupName) throws PersistenceException;

	/**
	 * Loads meta-data items for top-level groups.
	 * 
	 * @return a list of meta-data items or an empty list, if no top-level groups exist
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	List<Map<String, String>> loadMetadataForTopLevelGroups() throws PersistenceException;

	/**
	 * Loads meta-data items for child groups.
	 * 
	 * @param groupName
	 *            parent group name
	 * @return a list of meta-data items or an empty list, if no child groups found or such parent group does not exist
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	List<Map<String, String>> loadMetadataForChildGroups(String groupName) throws PersistenceException;
}
