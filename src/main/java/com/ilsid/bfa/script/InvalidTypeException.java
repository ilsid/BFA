package com.ilsid.bfa.script;

/**
 * Signals that error occurred when resolving a type of some scripting abstraction.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class InvalidTypeException extends ScriptException {

	public InvalidTypeException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidTypeException(String message) {
		super(message);
	}

}
