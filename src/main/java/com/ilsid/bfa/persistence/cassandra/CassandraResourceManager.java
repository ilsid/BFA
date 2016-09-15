package com.ilsid.bfa.persistence.cassandra;

/**
 * Provides facilities for management of Cassandra resources.
 * 
 * @author illia.sydorovych
 *
 */
public class CassandraResourceManager {
	
	/**
	 * Releases resources.
	 */
	public static void releaseResources() {
		CassandraRepository.closeConnection();
	}

}
