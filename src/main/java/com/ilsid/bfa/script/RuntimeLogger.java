package com.ilsid.bfa.script;

import org.slf4j.Logger;

/**
 * Logger for scripting runtime.
 * 
 * @author illia.sydorovych
 *
 */
public class RuntimeLogger {

	private final Logger logger;

	private final String messagePrefix;

	/**
	 * Creates instance
	 * 
	 * @param logger
	 *            logger implementation
	 * @param messagePrefix
	 *            message prefix. All messages will be written with this prefix
	 */
	public RuntimeLogger(Logger logger, String messagePrefix) {
		this.logger = logger;
		this.messagePrefix = messagePrefix;
	}

	/**
	 * Logs DEBUG messages.
	 * 
	 * @param message
	 *            message to log
	 */
	public void debug(String message) {
		logger.debug("{}: {}", messagePrefix, message);
	}
}
