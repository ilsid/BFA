package com.ilsid.bfa.runtime.persistence;

import java.util.Collection;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.persistence.PersistenceException;
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
	 * @throws IllegalArgumentException
	 *             if not all required fields are populated
	 */
	void createRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException, IllegalArgumentException;

	/**
	 * Updates runtime record in the repository.
	 * 
	 * @param record
	 *            a record to update
	 * @throws PersistenceException
	 *             in case of any repository issues
	 * @throws IllegalArgumentException
	 *             if <i>runtime ID</i> is not populated
	 */
	void updateRuntimeRecord(ScriptRuntimeDTO record) throws PersistenceException, IllegalArgumentException;

	/**
	 * Fetches runtime records by the given criteria. Result is paginated.
	 * 
	 * @param criteria
	 *            query criteria
	 * @param pagingOptions
	 *            pagination options
	 * @return a collection of runtime records or an empty list if no records fit the criteria
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	Collection<ScriptRuntimeDTO> fetch(ScriptRuntimeCriteria criteria, QueryPagingOptions pagingOptions)
			throws PersistenceException;
}
