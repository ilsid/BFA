package com.ilsid.bfa.persistence.filesystem;

import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Signals that an error occurred while trying to lock a resource.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class LockException extends PersistenceException {

	public LockException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockException(String message) {
		super(message);
	}

}
