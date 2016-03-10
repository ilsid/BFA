package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.manager.ScriptManager;
import com.ilsid.bfa.service.dto.TypeAdminParams;

public abstract class AbstractAdminResource {

	private static final String EMPTY = "";
	
	protected final static String GROUP_PARAM = "group";
	
	protected final static String NAME_PARAM = "name";
	
	protected static final String FILE_PARAM = "file";

	protected ScriptManager scriptManager;

	@Inject
	public void setScriptManager(ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

	protected void validateNonEmptyNameAndBody(String path, TypeAdminParams params) {
		if (isEmpty(params.getName())) {
			throw new ResourceException(path, "The name must be defined", Status.BAD_REQUEST);
		}
		if (isEmpty(params.getBody())) {
			throw new ResourceException(path, "The body must be defined", Status.BAD_REQUEST);
		}
	}

	protected void validateNonEmptyName(String path, TypeAdminParams params) {
		if (isEmpty(params.getName())) {
			throw new ResourceException(path, "The name must be defined", Status.BAD_REQUEST);
		}
	}

	protected void validateNonEmptyParameter(String path, String name, Object value) {
		if (isEmpty(value)) {
			throw new ResourceException(path, String.format("The value of [%s] parameter must be not empty", name),
					Status.BAD_REQUEST);
		}
	}
	
	protected void validateNonNullParameter(String path, String name, Object value) {
		if (value == null) {
			throw new ResourceException(path, String.format("The value of [%s] parameter must be not null", name),
					Status.BAD_REQUEST);
		}
	}

	private boolean isEmpty(Object value) {
		return (value == null || EMPTY.equals(value));
	}

}
