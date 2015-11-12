package com.ilsid.bfa.service.dto;

public class ScriptRuntimeStatus {
	
	private RuntimeStatusType statusType;
	
	private String errorDetails;
	
	public ScriptRuntimeStatus(RuntimeStatusType statusType, String errorDetails) {
		this.statusType = statusType;
		this.errorDetails = errorDetails;
	}

	public ScriptRuntimeStatus(RuntimeStatusType statusType) {
		this.statusType = statusType;
	}

	public ScriptRuntimeStatus() {
		
	}
	
	public RuntimeStatusType getStatusType() {
		return statusType;
	}

	public String getErrorDetails() {
		return errorDetails;
	}
	
	
}
