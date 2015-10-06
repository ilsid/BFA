package com.ilsid.bfa.persistence;

/**
 * Represents a repository for a dynamic code.
 * 
 * @author illia.sydorovych
 *
 */
public interface DynamicCodeRepository {

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
	public byte[] load(String className) throws PersistenceException;

}
