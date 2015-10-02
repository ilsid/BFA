package com.ilsid.bfa.script;

/**
 * Signals that error occurred when parsing or compiling dynamic scripting code.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class DynamicCodeException extends ScriptException {

	/**
	 * 
	 * @param message
	 * @param cause
	 */
	public DynamicCodeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * 
	 * @param message
	 */
	public DynamicCodeException(String message) {
		super(message);
	}

}
