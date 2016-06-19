package com.ilsid.bfa.persistence.filesystem;

import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.persistence.TransactionManager;

/**
 * Provides a file system based implementation that locks/unlocks a repository in exclusive mode.
 * 
 * @author illia.sydorovych
 *
 */
public class FSTransactionManager implements TransactionManager {
	
	private FilesystemRepository repository;
	
	public FSTransactionManager(FilesystemRepository repository) {
		this.repository = repository;
	}

	public boolean isTransactionStarted() {
		return repository.isLocked();
	}
	
	/**
	 * Locks a repository.
	 * 
	 * @throws PersistenceException
	 *             if a repository has been already locked
	 */
	public void startTransaction() throws PersistenceException {
		repository.lock();
	}
	
	/**
	 * Unlocks a repository.
	 */
	public void commitTransaction() throws PersistenceException {
		repository.unlock();
	}
	
	/**
	 * Does the same as {@link #commitTransaction()}.
	 */
	public void rollbackTransaction() {
		repository.unlock();
	}

}
