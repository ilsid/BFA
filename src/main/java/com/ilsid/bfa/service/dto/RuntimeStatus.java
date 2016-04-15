package com.ilsid.bfa.service.dto;

import com.ilsid.bfa.runtime.dto.RuntimeStatusType;

/**
 * Represents the script runtime state.
 * 
 * @author illia.sydorovych
 *
 */
public class RuntimeStatus {

	private RuntimeStatusType statusType;

	private String errorDetails;

	private long runtimeId;

	public static RuntimeStatusBuilder runtimeId(long runtimeId) {
		return new RuntimeStatusBuilder(runtimeId);
	}

	/**
	 * Returns the unique runtime identifier for the script.
	 * 
	 * @return the runtime identifier
	 */
	public long getRuntimeId() {
		return runtimeId;
	}

	/**
	 * Returns the current script status.
	 * 
	 * @return the current status
	 */
	public RuntimeStatusType getStatusType() {
		return statusType;
	}

	/**
	 * Returns the runtime error details. This value is not <code>null</code> only when the status is equal to
	 * {@link RuntimeStatusType#FAILED}.
	 * 
	 * @return the runtime error details for the failed script
	 */
	public String getErrorDetails() {
		return errorDetails;
	}

	public static class RuntimeStatusBuilder {

		private RuntimeStatus instance;

		public RuntimeStatusBuilder(long runtimeId) {
			instance = new RuntimeStatus();
			instance.runtimeId = runtimeId;
		}

		public RuntimeStatusBuilder statusType(RuntimeStatusType statusType) {
			instance.statusType = statusType;
			return this;
		}

		public RuntimeStatusBuilder errorDetails(String errorDetails) {
			instance.errorDetails = errorDetails;
			return this;
		}

		public RuntimeStatus build() {
			return instance;
		}

	}

}
