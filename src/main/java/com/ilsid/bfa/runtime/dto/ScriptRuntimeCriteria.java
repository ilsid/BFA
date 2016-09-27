package com.ilsid.bfa.runtime.dto;

import java.util.Date;

/**
 * Criteria for queries fetching Runtime records.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptRuntimeCriteria {

	private Date startDate;

	private RuntimeStatusType status;

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

}
