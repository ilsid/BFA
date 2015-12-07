package com.ilsid.bfa;

import java.util.Map;

/**
 * Represents a configurable abstraction.
 * 
 * @author illia.sydorovych
 *
 */
public interface Configurable {

	/**
	 * Defines a configuration specific to some implementation.
	 * 
	 * @param config
	 *            a configuration
	 * @throws ConfigurationException
	 *             if the passed configuration is not valid for the given implementation
	 */
	void setConfiguration(Map<String, String> config) throws ConfigurationException;

}
