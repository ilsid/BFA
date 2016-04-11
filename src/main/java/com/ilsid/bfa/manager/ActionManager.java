package com.ilsid.bfa.manager;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.ilsid.bfa.action.persistence.ActionInfo;
import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.GroupNameUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.filesystem.MetadataUtil;

/**
 * Provides a set of management operations for actions.
 * 
 * @author illia.sydorovych
 *
 */
@Singleton
public class ActionManager {

	private ActionRepository repository;

	/**
	 * Creates new script group.
	 * 
	 * @param groupName
	 *            the script group name
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if such group already exists in the repository</li>
	 *             <li>if parent group does not exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void createGroup(String groupName) throws ManagementException {
		try {
			repository.createGroup(groupName, createActionGroupMetadata(groupName));
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to created the action group [%s]", groupName), e);
		}
	}

	/**
	 * Loads meta-data items for all top-level action groups.
	 * 
	 * @return a list of meta-data items
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if no top-level groups exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public List<Map<String, String>> getTopLevelActionGroupMetadatas() throws ManagementException {
		List<Map<String, String>> result;
		try {
			result = repository.loadMetadataForTopLevelGroups();
		} catch (PersistenceException e) {
			throw new ManagementException("Failed to load the info for top-level action groups", e);
		}

		if (result.isEmpty()) {
			throw new ManagementException("No top-level action groups found");
		}

		MetadataUtil.addParentRecord(result, Metadata.ROOT_PARENT_NAME);

		return result;
	}

	/**
	 * Loads meta-data items for sub-groups and actions in the specified group.
	 * 
	 * @param groupName
	 *            a group name
	 * @return a list of meta-data items or an empty list, if no sub-groups or actions found or such group does not
	 *         exist
	 * @throws ManagementException
	 *             in case of any repository access issues
	 */
	public List<Map<String, String>> getChildrenActionGroupMetadatas(String groupName) throws ManagementException {
		List<Map<String, String>> result;
		try {
			result = repository.loadMetadataForChildGroups(groupName);
			result.addAll(repository.loadMetadataForActions(groupName));
		} catch (PersistenceException e) {
			throw new ManagementException(
					String.format("Failed to load children meta-data for the action group [%s]", groupName), e);
		}

		if (!result.isEmpty()) {
			MetadataUtil.addParentRecord(result, groupName);
		}

		return result;
	}

	/**
	 * Saves new action in the repository.
	 * 
	 * @param actionName
	 *            action name
	 * @param actionPackage
	 *            action package
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if action with the given name already exists in the repository</li>
	 *             <li>if action group does not exist in the repository</li>
	 *             <li>if action package has invalid format</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public void createAction(String actionName, InputStream actionPackage) throws ManagementException {
		try {
			repository.save(actionName, actionPackage);
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to save the action [%s]", actionName), e);
		}
	}

	/**
	 * Provides details for the given action.
	 * 
	 * @param actionName
	 *            action name
	 * @return action details
	 * @throws ManagementException
	 *             <ul>
	 *             <li>if action with the given name does not exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	public ActionDetails getDetails(String actionName) throws ManagementException {
		ActionInfo info;
		try {
			info = repository.loadInfo(actionName);
		} catch (PersistenceException e) {
			throw new ManagementException(String.format("Failed to load details for the action [%s]", actionName), e);
		}

		if (info == null) {
			throw new ManagementException(
					String.format("The action [%s] does not exist in the repository", actionName));
		}

		ActionDetails result = new ActionDetails();
		result.setImplementationClassName(info.getImplementationClassName());
		result.setDependencies(info.getDependencies());

		return result;
	}

	/**
	 * Defines a repository implementation.
	 * 
	 * @param repository
	 *            actions repository
	 */
	@Inject
	public void setRepository(ActionRepository repository) {
		this.repository = repository;
	}

	/**
	 * Action details.
	 */
	public static class ActionDetails {

		private String implementationClassName;

		private List<String> dependencies;

		public String getImplementationClassName() {
			return implementationClassName;
		}

		public List<String> getDependencies() {
			return dependencies;
		}
		
		void setImplementationClassName(String implementationClassName) {
			this.implementationClassName = implementationClassName;
		}

	    void setDependencies(List<String> dependencies) {
			this.dependencies = dependencies;
		}
	}

	private Map<String, String> createActionGroupMetadata(String groupName) {
		Map<String, String> metaData = new LinkedHashMap<>();
		metaData.put(Metadata.TYPE, Metadata.ACTION_GROUP_TYPE);
		metaData.put(Metadata.NAME, groupName);
		metaData.put(Metadata.TITLE, GroupNameUtil.splitName(groupName).getChildName());

		return metaData;
	}

}
