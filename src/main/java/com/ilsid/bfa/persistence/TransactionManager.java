package com.ilsid.bfa.persistence;

public interface TransactionManager {
	
	boolean isTransactionStarted();
	
	void startTransaction() throws PersistenceException;
	
	void commitTransaction() throws PersistenceException;
	
	void rollbackTransaction();

}
