package com.ilsid.bfa.service.dto;

public class ScriptRuntimeParams {
	
	private String name;
	
	private Object[] inputParameters = new Object[] {};
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Object[] getInputParameters() {
		return inputParameters;
	}
	
	public void setInputParameters(Object[] inputParameters) {
		this.inputParameters = inputParameters;
	}
	
}
