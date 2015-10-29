package com.ilsid.bfa.persistence.filesystem;

import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.TransactionManager;

/**
 * Provides the stub implementation that does not support transactions.
 * 
 * @author illia.sydorovych
 *
 */
public class FSTransactionManager implements TransactionManager {
	
	private static final FSTransactionManager instance = new FSTransactionManager();
	
	private FSTransactionManager() {
		
	}
	
	public static TransactionManager getInstance() {
		return instance;
	}
	
	/**
	 * Returns <code>false</code>.
	 */
	public boolean isTransactionStarted() {
		return false;
	}

	public void startTransaction() throws PersistenceException {
	}

	public void commitTransaction() throws PersistenceException {
	}

	public void rollbackTransaction() {
	}

}
