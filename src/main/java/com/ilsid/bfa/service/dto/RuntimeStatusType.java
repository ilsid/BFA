package com.ilsid.bfa.service.dto;

public enum RuntimeStatusType {

	INPROGRESS("In Progress"), COMPLETED("Completed"), FAILED("Failed");

	private final String value;

	RuntimeStatusType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

}
