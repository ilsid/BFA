package com.ilsid.bfa.service.server;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.common.ExceptionUtil;

/**
 * Signals that a resource method failure occurred.
 * 
 * @author illia.sydorovych
 *
 */
public class ResourceException extends WebApplicationException {

	private static final long serialVersionUID = -3798165233403070956L;

	private String message;

	private Status status = Status.INTERNAL_SERVER_ERROR;

	private boolean hasCause;

	private String path;

	private Object entity;

	public ResourceException(String path, Throwable e) {
		super(e);
		this.path = path;
		hasCause = true;
	}

	public ResourceException(String path, Throwable e, Status status, Object entity) {
		super(e);
		this.path = path;
		hasCause = true;
		this.status = status;
		this.entity = entity;
	}

	public ResourceException(String path, String message, Status status) {
		this.path = path;
		this.message = message;
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public String getPath() {
		return path;
	}

	public Status getStatus() {
		return status;
	}

	public boolean hasCause() {
		return hasCause;
	}

	public Throwable getActualCause() {
		return hasCause ? this.getCause() : this;
	}

	public Object getEntity() {
		if (entity == null) {
			return ExceptionUtil.getExceptionMessageChain(getActualCause());
		} else {
			return entity;
		}
	}

}
