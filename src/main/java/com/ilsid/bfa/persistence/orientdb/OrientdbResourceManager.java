package com.ilsid.bfa.persistence.orientdb;

/**
 * Provides facilities for management of OrientDB resources.
 * 
 * @author illia.sydorovych
 *
 */
public class OrientdbResourceManager {
	
	/**
	 * Releases resources.
	 */
	public static void releaseResources() {
		OrientdbRepository.closeFactory();
	}

}
