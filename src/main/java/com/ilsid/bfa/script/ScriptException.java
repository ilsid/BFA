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
	
	private Object flowRuntimeId;

	public ScriptException(String message, Throwable cause) {
		super(message, cause);
	}

	public ScriptException(String message) {
		super(message);
	}
	
	public ScriptException(String message, Throwable cause, Object flowRuntimeId) {
		super(message, cause);
		this.flowRuntimeId = flowRuntimeId;
	}

	public ScriptException(String message, Object flowRuntimeId) {
		super(message);
		this.flowRuntimeId = flowRuntimeId;
	}
	
	public Object getFlowRuntimeId() {
		return flowRuntimeId;
	}

}
