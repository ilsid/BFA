package com.ilsid.bfa.persistence;

/**
 * Repository that supports transactions.
 * 
 * @author illia.sydorovych
 *
 */
public interface Transactional {

	/**
	 * Returns a {@link TransactionManager} implementation.
	 * 
	 * @return a transaction manager for this repository
	 */
	TransactionManager getTransactionManager();

}
