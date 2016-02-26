package com.ilsid.bfa.manager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.ilsid.bfa.action.persistence.ActionRepository;
import com.ilsid.bfa.common.GroupNameUtil;
import com.ilsid.bfa.common.Metadata;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Provides a set of management operations for actions.
 * 
 * @author illia.sydorovych
 *
 */
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
	 * Defines a repository implementation.
	 * 
	 * @param repository
	 *            actions repository
	 */
	@Inject
	public void setRepository(ActionRepository repository) {
		this.repository = repository;
	}

	private Map<String, String> createActionGroupMetadata(String groupName) throws ManagementException {
		Map<String, String> metaData = new HashMap<>();
		metaData.put(Metadata.TYPE, Metadata.ACTION_GROUP_TYPE);
		metaData.put(Metadata.NAME, groupName);
		metaData.put(Metadata.TITLE, GroupNameUtil.splitName(groupName).getChildName());

		return metaData;
	}

}
