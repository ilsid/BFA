package com.ilsid.bfa.script;

import com.ilsid.bfa.BFAException;

/**
 * Signals that error occurred when parsing the scripting code.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class ParsingException extends BFAException {

	/**
	 * 
	 * @param message
	 * @param cause
	 */
	public ParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 
	 * @param message
	 */
	public ParsingException(String message) {
		super(message);
	}

}
