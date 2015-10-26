package com.ilsid.bfa.manager;

import com.ilsid.bfa.BFAException;

/**
 * Signals that a failure occurred while performing some management operation.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class ManagementException extends BFAException {

	public ManagementException(String message, Throwable cause) {
		super(message, cause);
	}

	public ManagementException(String message) {
		super(message);
	}

}
