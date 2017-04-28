package com.ilsid.bfa.service.dto;

import com.ilsid.bfa.persistence.QueryPagingOptions;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeCriteria;

public class ScriptRuntimeQuery {

	private ScriptRuntimeCriteria criteria;

	private int resultsPerPage = QueryPagingOptions.DEFAULT_RESULTS_PER_PAGE;

	private String pageToken;

	public ScriptRuntimeCriteria getCriteria() {
		return criteria;
	}

	public void setCriteria(ScriptRuntimeCriteria criteria) {
		this.criteria = criteria;
	}

	public int getResultsPerPage() {
		return resultsPerPage;
	}

	public void setResultsPerPage(int resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
	}

	public String getPageToken() {
		return pageToken;
	}

	public void setPageToken(String pageToken) {
		this.pageToken = pageToken;
	}

}
