package com.ilsid.bfa.persistence;

import java.util.List;
import java.util.Map;

import com.ilsid.bfa.Configurable;

/**
 * The scripting code repository.
 * 
 * @author illia.sydorovych
 *
 */
public interface ScriptingRepository extends Configurable {

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
	 * Saves the meta-data for the given class. Overwrites the existing meta-data, if any.
	 * 
	 * @param className
	 *            class name
	 * @param metaData
	 *            meta-data
	 * @return <code>true</code> if the meta-data was saved and <code>false</code> otherwise because the class with the
	 *         given name does not exist
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 * 
	 */
	boolean saveMetadata(String className, Map<String, String> metaData) throws PersistenceException;

	/**
	 * Saves new package.
	 * 
	 * @param packageName
	 *            the package name
	 * @param metaData
	 *            the package meta-data
	 * @throws PersistenceException
	 *             <ul>
	 *             <li>if the package with the given name already exists in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             </ul>
	 */
	void savePackage(String packageName, Map<String, String> metaData) throws PersistenceException;

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
	 * Deletes the class and its source, if any.
	 * 
	 * @param className
	 *            the name of class to delete
	 * @return a number of deleted objects
	 *         <ul>
	 *         <li><code>0</code> is returned, if the class with the given name does not exist</li>
	 *         <li><code>1</code> is returned if the class with the given name was deleted, but no corresponding source
	 *         was found</li>
	 *         <li><code>2</code> is returned if both class and source were deleted</li>
	 *         </ul>
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	int deleteClass(String className) throws PersistenceException;

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
	// FIXME: move from repository API
	long getNextRuntimeId() throws PersistenceException;

	/**
	 * Loads meta-data items for top-level packages.
	 * 
	 * @return a list of meta-data items or an empty list, if no top-level packages found
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	List<Map<String, String>> loadMetadataForTopLevelPackages() throws PersistenceException;

	/**
	 * Loads meta-data items for child packages.
	 * 
	 * @param packageName
	 *            parent package name
	 * @return a list of meta-data items or an empty list, if no child packages found or such parent package does not
	 *         exist
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	List<Map<String, String>> loadMetadataForChildPackages(String packageName) throws PersistenceException;

	/**
	 * Loads meta-data for the package.
	 * 
	 * @param packageName
	 *            package name
	 * @return the meta-data for the given package or <code>null</code> if such package does not exist or it has no the
	 *         corresponding meta-data
	 * @throws PersistenceException
	 *             in case of any repository access issues
	 */
	Map<String, String> loadMetadataForPackage(String packageName) throws PersistenceException;

	/**
	 * Returns a proper {@link TransactionManager} instance for this code repository.
	 * 
	 * @return a transaction manager for this repository
	 */
	TransactionManager getTransactionManager();

}
