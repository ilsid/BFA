package com.ilsid.bfa.runtime.monitor;

import com.ilsid.bfa.BFAException;

/**
 * Signals that a monitoring error occurred.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class MonitoringException extends BFAException {

	public MonitoringException(String message) {
		super(message);
	}

	public MonitoringException(String message, Throwable cause) {
		super(message, cause);
	}

}
