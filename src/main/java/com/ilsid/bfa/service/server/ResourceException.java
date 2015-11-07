package com.ilsid.bfa.service.server;

import javax.ws.rs.WebApplicationException;

/**
 * Signals that a resource method failure occurred.
 * 
 * @author illia.sydorovych
 *
 */
public class ResourceException extends WebApplicationException {

	private static final long serialVersionUID = -3798165233403070956L;

	private String message;

	public ResourceException(String message, Throwable e) {
		super(e);
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}
