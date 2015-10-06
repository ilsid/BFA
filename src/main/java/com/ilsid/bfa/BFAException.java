package com.ilsid.bfa;

/**
 * Base class for all exceptions.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class BFAException extends Exception {

	public BFAException(String message, Throwable cause) {
		super(message, cause);
	}

	public BFAException(String message) {
		super(message);
	}

}
