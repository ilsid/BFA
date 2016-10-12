package com.ilsid.bfa.runtime.dto;

import java.util.Collection;
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
	private Object runtimeId;

	@RequiredField
	private String userName;

	@RequiredField
	private String scriptName;

	private List<String> parameters;

	@RequiredField
	private RuntimeStatusType status;

	@RequiredField
	private Date startTime;

	private Date endTime;

	private Collection<String> callStack;

	private Collection<String> errorDetails;

	public Object getRuntimeId() {
		return runtimeId;
	}

	public ScriptRuntimeDTO setRuntimeId(Object runtimeId) {
		this.runtimeId = runtimeId;
		return this;
	}

	public String getUserName() {
		return userName;
	}

	public ScriptRuntimeDTO setUserName(String userName) {
		this.userName = userName;
		return this;
	}

	public String getScriptName() {
		return scriptName;
	}

	public ScriptRuntimeDTO setScriptName(String scriptName) {
		this.scriptName = scriptName;
		return this;
	}

	public List<String> getParameters() {
		return parameters;
	}

	public ScriptRuntimeDTO setParameters(List<String> parameters) {
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

	public Collection<String> getCallStack() {
		return callStack;
	}

	public ScriptRuntimeDTO setCallStack(Collection<String> callStack) {
		this.callStack = callStack;
		return this;
	}

	public Collection<String> getErrorDetails() {
		return errorDetails;
	}

	public ScriptRuntimeDTO setErrorDetails(Collection<String> error) {
		this.errorDetails = error;
		return this;
	}

}
