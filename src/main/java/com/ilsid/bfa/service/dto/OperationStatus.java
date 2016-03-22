package com.ilsid.bfa.service.dto;

/**
 * Operation status.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class OperationStatus {

	public static final OperationStatus SUCCESS = new Success();

	public String getValue() {
		throw new RuntimeException("Must be implemented by descendant");
	}

	/**
	 * Success.
	 * 
	 * @author illia.sydorovych
	 *
	 */
	public static class Success extends OperationStatus {

		private static final String VAL_OK = "OK";

		private String value = VAL_OK;

		@Override
		public String getValue() {
			return value;
		}

	}

	/**
	 * Failure.
	 * 
	 * @author illia.sydorovych
	 *
	 */
	public static class Failure extends OperationStatus {

		private static final String PREFIX = "Error: ";

		private String value;

		public Failure() {
		}

		public Failure(String errorMessage) {
			value = PREFIX.concat(errorMessage);
		}

		@Override
		public String getValue() {
			return value;
		}
	}

}
