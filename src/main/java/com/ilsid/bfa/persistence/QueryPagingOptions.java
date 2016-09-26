package com.ilsid.bfa.persistence;

/**
 * Options for queries with pagination support.
 * 
 * @author illia.sydorovych
 *
 */
public class QueryPagingOptions {

	private static final int DEFAULT_RESULTS_PER_PAGE = 50;

	private int resultsPerPage = DEFAULT_RESULTS_PER_PAGE;

	private Object pageToken;

	public int getResultsPerPage() {
		return resultsPerPage;
	}

	public QueryPagingOptions setResultsPerPage(int resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
		return this;
	}

	public Object getPageToken() {
		return pageToken;
	}

	public QueryPagingOptions setPageToken(Object pageToken) {
		this.pageToken = pageToken;
		return this;
	}

}
