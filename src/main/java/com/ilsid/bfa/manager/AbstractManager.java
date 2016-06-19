package com.ilsid.bfa.manager;

import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.TransactionManager;

/**
 * Base class for management operations.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class AbstractManager {

	protected abstract TransactionManager getTransactionManager();

	protected void startTransaction() throws PersistenceException {
		getTransactionManager().startTransaction();
	}

	protected void commitTransaction() throws PersistenceException {
		getTransactionManager().commitTransaction();
	}

	protected void rollbackTransaction() {
		TransactionManager txManager = getTransactionManager();
		if (txManager.isTransactionStarted()) {
			txManager.rollbackTransaction();
		}
	}

}
