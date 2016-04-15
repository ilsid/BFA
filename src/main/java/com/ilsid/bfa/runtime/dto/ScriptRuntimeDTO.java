package com.ilsid.bfa.runtime.dto;

import java.util.Date;
import java.util.List;

import com.ilsid.bfa.dto.RequiredField;

/**
 * Script runtime DTO.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptRuntimeDTO {

	@RequiredField
	private long runtimeId;
	
	@RequiredField
	private String scriptName;

	private List<Object> parameters;

	@RequiredField
	private RuntimeStatusType status;
	
	@RequiredField
	private Date startTime;

	private Date endTime;

	private Exception error;

	public long getRuntimeId() {
		return runtimeId;
	}

	public ScriptRuntimeDTO setRuntimeId(long runtimeId) {
		this.runtimeId = runtimeId;
		return this;
	}

	public String getScriptName() {
		return scriptName;
	}

	public ScriptRuntimeDTO setScriptName(String scriptName) {
		this.scriptName = scriptName;
		return this;
	}

	public List<Object> getParameters() {
		return parameters;
	}

	public ScriptRuntimeDTO setParameters(List<Object> parameters) {
		this.parameters = parameters;
		return this;
	}

	public RuntimeStatusType getStatus() {
		return status;
	}

	public ScriptRuntimeDTO setStatus(RuntimeStatusType status) {
		this.status = status;
		return this;
	}

	public Date getStartTime() {
		return startTime;
	}

	public ScriptRuntimeDTO setStartTime(Date startTime) {
		this.startTime = startTime;
		return this;
	}

	public Date getEndTime() {
		return endTime;
	}

	public ScriptRuntimeDTO setEndTime(Date endTime) {
		this.endTime = endTime;
		return this;
	}

	public Exception getError() {
		return error;
	}

	public ScriptRuntimeDTO setError(Exception error) {
		this.error = error;
		return this;
	}

}
