package com.ilsid.bfa.action;

import com.ilsid.bfa.BFAException;

/**
 * Signals that an action execution failure occurred.
 * 
 * @author illia.sydorovych
 *
 */
public class ActionException extends BFAException {

	private static final long serialVersionUID = -3091817662184893454L;

	public ActionException(String message, Throwable cause) {
		super(message, cause);
	}

	public ActionException(String message) {
		super(message);
	}

}
