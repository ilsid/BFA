package com.ilsid.bfa.compiler;

import com.ilsid.bfa.BFAException;

/**
 * Signals that compilation failure occurred.
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class ClassCompilationException extends BFAException {

	public ClassCompilationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClassCompilationException(String message) {
		super(message);
	}

}
