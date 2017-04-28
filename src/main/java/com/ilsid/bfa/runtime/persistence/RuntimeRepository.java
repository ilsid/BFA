package com.ilsid.bfa.runtime.persistence;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.QueryPage;
import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;

/**
 * The repository that provides the scripts runtime information.
 * 
 * @author illia.sydorovych
 *
 */
public interface RuntimeRepository extends Configurable {

	/**
	 * Generates new unique script runtime id.
	 * 
	 * @return the runtime id value
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	Object getNextRuntimeId() throws PersistenceException;

	/**
	 * Creates runtime record in the repository.
	 * 
	 * @param record
	 *            a record to save
	 * @throws PersistenceException
	 *             in case of any repository issues
	 */
	void createRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException;

	/**
	 * Updates runtime record in the repository.
	 * 
	 * @param record
	 *            a record to update
	 * @throws PersistenceException
	 *             in case of any repository issues
	 */
	void updateRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException;

	/**
	 * Fetches runtime records by the given criteria. Result is paginated.
	 * 
	 * @param criteria
	 *            query criteria
	 * @param pagingOptions
	 *            pagination options
	 * @return {@link QueryPage} instance
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	QueryPage<ScriptRuntimeDTO> fetch(ScriptRuntimeCriteria criteria, QueryPagingOptions pagingOptions)
			throws PersistenceException;
}
