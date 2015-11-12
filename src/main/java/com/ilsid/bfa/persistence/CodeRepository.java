package com.ilsid.bfa.persistence;

import java.util.Map;

import com.ilsid.bfa.ConfigurationException;

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
	 * @return a byte array representing Java byte code or <code>null</code>, if a class with such name does not exist
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 * 
	 */
	byte[] load(String className) throws PersistenceException;

	/**
	 * Saves new class. Source code here is not the whole code of the class, but the variable part only (for example, a
	 * script body), that may be updated by user in the future.
	 * 
	 * @param className
	 *            the class name
	 * @param byteCode
	 *            the class byte code
	 * @param sourceCode
	 *            the source code of the variable part
	 * @throws PersistenceException
	 *             <ul>
	 *             <li>if the class with the given name already exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	void save(String className, byte[] byteCode, String sourceCode) throws PersistenceException;

	/**
	 * Saves new class.
	 * 
	 * @param className
	 *            the class name
	 * @param byteCode
	 *            the class byte code
	 * @throws PersistenceException
	 *             <ul>
	 *             <li>if the class with the given name already exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	void save(String className, byte[] byteCode) throws PersistenceException;

	/**
	 * Deletes all classes under the specified package.
	 * 
	 * @param packageName
	 *            package to delete
	 * @return a number of deleted classes. If no classes under the given package were found, then <code>0</code> is
	 *         returned.
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	int deletePackage(String packageName) throws PersistenceException;

	/**
	 * Loads the source code for the script defined by the given class name. The source code means here the variable
	 * part only (the script body).
	 * 
	 * @param className
	 *            the script's class name including the full package
	 * @return the source code or <code>null</code>, if the script with such class name is not found
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	String loadSourceCode(String className) throws PersistenceException;

	/**
	 * Generates new unique script runtime id.
	 * 
	 * @return the runtime id value
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	long getNextRuntimeId() throws PersistenceException;

	/**
	 * Returns a proper {@link TransactionManager} instance for this code repository.
	 * 
	 * @return a transaction manager for this repository
	 */
	TransactionManager getTransactionManager();

	/**
	 * Defines a configuration specific to this repository
	 * 
	 * @param config
	 *            a repository configuration
	 * @throws ConfigurationException
	 *             if the passed configuration is not valid for this repository
	 */
	void setConfiguration(Map<String, String> config) throws ConfigurationException;
}
