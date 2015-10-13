package com.ilsid.bfa.persistence;

/**
 * Represents a code repository.
 * 
 * @author illia.sydorovych
 *
 */
//TODO: complete javadocs
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
	public byte[] load(String className) throws PersistenceException;
	
	public void save(String className, byte[] byteCode) throws PersistenceException;
	
	public void update(String className, byte[] byteCode) throws PersistenceException;
	
	public void delete(String className) throws PersistenceException;
}
