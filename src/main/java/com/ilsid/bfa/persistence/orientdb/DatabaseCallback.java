package com.ilsid.bfa.persistence.orientdb;

import com.ilsid.bfa.persistence.PersistenceException;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/**
 * A callback for database operations.
 * 
 * @author illia.sydorovych
 *
 */
public interface DatabaseCallback<T> {

	/**
	 * Executes database operation.
	 * 
	 * @param connection
	 *            a database connection
	 * @throws PersistenceException
	 *             in case of operation failure
	 * @return operation result
	 */
	T doInDatabase(OrientGraph connection) throws PersistenceException;

}
