package com.ilsid.bfa.service.server;

import javax.inject.Inject;

import com.ilsid.bfa.manager.ScriptManager;
import com.ilsid.bfa.service.dto.TypeAdminParams;

public abstract class AbstractAdminResource {
	
	protected ScriptManager scriptManager;
	
	@Inject
	public void setScriptManager(ScriptManager scriptManager) {
		this.scriptManager = scriptManager;
	}

	
	protected void validateNonNullNameAndBody(TypeAdminParams params) {
		if (params.getName() == null) {
			throw new IllegalArgumentException("The name must be defined");
		}
		if (params.getBody() == null) {
			throw new IllegalArgumentException("The body must be defined");
		}
	}
	
	protected void validateNonNullName(TypeAdminParams params) {
		if (params.getName() == null) {
			throw new IllegalArgumentException("The name must be defined");
		}
	}

}
