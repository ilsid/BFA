package com.ilsid.bfa.runtime.dto;

import java.util.Date;

/**
 * Criteria for runtime queries.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptRuntimeCriteria {

	private Date startDate;

	private RuntimeStatusType status;

	private Date minStartTime;

	private Date maxStartTime;

	public Date getStartDate() {
		return startDate;
	}

	public ScriptRuntimeCriteria setStartDate(Date startDate) {
		this.startDate = startDate;
		return this;
	}

	public RuntimeStatusType getStatus() {
		return status;
	}

	public ScriptRuntimeCriteria setStatus(RuntimeStatusType status) {
		this.status = status;
		return this;
	}

	public Date getMinStartTime() {
		return minStartTime;
	}

	public ScriptRuntimeCriteria setMinStartTime(Date minStartTime) {
		this.minStartTime = minStartTime;
		return this;
	}

	public Date getMaxStartTime() {
		return maxStartTime;
	}

	public ScriptRuntimeCriteria setMaxStartTime(Date maxStartTime) {
		this.maxStartTime = maxStartTime;
		return this;
	}
}
