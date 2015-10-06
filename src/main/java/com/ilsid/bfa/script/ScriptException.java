package com.ilsid.bfa.script;

import com.ilsid.bfa.BFAException;

/**
 * Signals that a script execution failure occurred.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class ScriptException extends BFAException {

	public ScriptException(String message, Throwable cause) {
		super(message, cause);
	}

	public ScriptException(String message) {
		super(message);
	}
	
	

}
