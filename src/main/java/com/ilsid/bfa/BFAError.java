package com.ilsid.bfa;

/**
 * Signals that some unrecoverable failure occurred;
 * 
 * @author illia.sydorovych
 *
 */
@SuppressWarnings("serial")
public class BFAError extends Error {

	public BFAError(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public BFAError(String arg0) {
		super(arg0);
	}

}
