package com.ilsid.bfa.persistence;

import java.util.List;

/**
 * Result for a query with pagination support.
 * 
 * @author illia.sydorovych
 *
 * @param <T>
 *            record type
 */
public class QueryPage<T> {

	private List<T> result;

	private String nextPageToken;

	public QueryPage(List<T> result) {
		this.result = result;
	}

	public QueryPage(List<T> result, String nextPageToken) {
		this.result = result;
		this.nextPageToken = nextPageToken;
	}

	/**
	 * Query result.
	 * 
	 * @return a list of records or an empty list if no records fit the criteria
	 */
	public List<T> getResult() {
		return result;
	};

	/**
	 * Returns the next page token.
	 * 
	 * @return a string representation of the next page token or <code>null</code> of no more pages are available
	 */
	public String getNextPageToken() {
		return nextPageToken;
	}
}
