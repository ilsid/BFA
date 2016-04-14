package com.ilsid.bfa.persistence.orientdb;

import com.ilsid.bfa.persistence.PersistenceException;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;

/**
 * A callback for database operations.
 * 
 * @author illia.sydorovych
 *
 */
public interface DatabaseCallback {

	/**
	 * Executes database operation.
	 * 
	 * @param connection
	 *            a database connection
	 * @throws PersistenceException
	 *             in case of operation failure
	 */
	void doInDatabase(OrientGraph connection) throws PersistenceException;

}
