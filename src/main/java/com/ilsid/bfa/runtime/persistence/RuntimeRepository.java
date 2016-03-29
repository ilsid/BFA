package com.ilsid.bfa.runtime.persistence;

import com.ilsid.bfa.Configurable;
import com.ilsid.bfa.persistence.PersistenceException;

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
	long getNextRuntimeId() throws PersistenceException;

}
