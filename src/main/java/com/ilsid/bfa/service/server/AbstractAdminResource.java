package com.ilsid.bfa.service.server;

import javax.inject.Inject;
import javax.ws.rs.core.Response.Status;

import com.ilsid.bfa.manager.ScriptManager;
import com.ilsid.bfa.service.dto.TypeAdminParams;

public abstract class AbstractAdminResource {

	protected ScriptManager scriptManager;

	@Inject
	public void setScriptManager(ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

	protected void validateNonNullNameAndBody(String path, TypeAdminParams params) {
		if (params.getName() == null) {
			throw new ResourceException(path, "The name must be defined", Status.BAD_REQUEST);
		}
		if (params.getBody() == null) {
			throw new ResourceException(path, "The body must be defined", Status.BAD_REQUEST);
		}
		if (params.getTitle() == null) {
			throw new ResourceException(path, "The title must be defined", Status.BAD_REQUEST);
		}
	}

	protected void validateNonNullName(String path, TypeAdminParams params) {
		if (params.getName() == null) {
			throw new ResourceException(path, "The name must be defined", Status.BAD_REQUEST);
		}
	}

	protected void validateNonNullParameter(String path, String name, Object value) {
		if (value == null) {
			throw new ResourceException(path, String.format("%s parameter must be defined", name), Status.BAD_REQUEST);
		}
	}

}
