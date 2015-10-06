package com.ilsid.bfa.persistence;

import com.ilsid.bfa.BFAException;

/**
 * Signals that a persistence error occurred.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class PersistenceException extends BFAException {

	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	public PersistenceException(String message) {
		super(message);
	}
	
	
}
