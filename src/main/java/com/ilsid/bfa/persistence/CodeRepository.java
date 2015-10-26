package com.ilsid.bfa.persistence;

/**
 * Represents a code repository.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: complete javadocs
public interface CodeRepository {

	/**
	 * Loads a byte code for a given class name.
	 * 
	 * @param className
	 *            class name
	 * @return a byte array representing Java byte code or an empty array if a
	 *         class with such name does not exist
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	byte[] load(String className) throws PersistenceException;

	/**
	 * Saves the class. Source code here is not the whole code of the class, but
	 * the variable part only (script body or dynamic expression), that may be
	 * updated by user in the future.
	 * 
	 * @param className
	 *            the class name
	 * @param byteCode
	 *            the class byte code
	 * @param sourceCode
	 *            the source code of the variable part
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	void save(String className, byte[] byteCode, String sourceCode) throws PersistenceException;

	void update(String className, byte[] byteCode, String sourceCode) throws PersistenceException;

	void delete(String className) throws PersistenceException;

	/**
	 * Returns a proper {@link TransactionManager} instance for this code
	 * repository.
	 * 
	 * @return a transaction manager for this repository
	 */
	TransactionManager getTransactionManager();
}
