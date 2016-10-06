package com.ilsid.bfa.persistence;

/**
 * Options for queries with pagination support.
 * 
 * @author illia.sydorovych
 *
 */
public class QueryPagingOptions {

	public static final int DEFAULT_RESULTS_PER_PAGE = 50;

	private static int defaultResultsPerPage = DEFAULT_RESULTS_PER_PAGE;

	private int resultsPerPage = defaultResultsPerPage;

	private String pageToken;

	public static synchronized void setDefaultResultsPerPage(int value) {
		defaultResultsPerPage = value;
	}

	public int getResultsPerPage() {
		return resultsPerPage;
	}

	public QueryPagingOptions setResultsPerPage(int resultsPerPage) {
		this.resultsPerPage = resultsPerPage;
		return this;
	}

	public String getPageToken() {
		return pageToken;
	}

	public QueryPagingOptions setPageToken(String pageToken) {
		this.pageToken = pageToken;
		return this;
	}

}
