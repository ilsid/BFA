package com.ilsid.bfa;

/**
 * Signals that a configuration related failure occurred.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class ConfigurationException extends BFAException {

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationException(String message) {
		super(message);
	}

}
