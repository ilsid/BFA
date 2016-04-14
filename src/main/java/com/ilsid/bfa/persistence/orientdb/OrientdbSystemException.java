package com.ilsid.bfa.persistence.orientdb;

import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Signals that OrientDB system failure occurred.
 * 
 * @author illia.sydorovych
 *
 */
public class OrientdbSystemException extends PersistenceException {

	private static final long serialVersionUID = -5152684972773317941L;

	public OrientdbSystemException(String message, Throwable cause) {
		super(message, cause);
	}

}
