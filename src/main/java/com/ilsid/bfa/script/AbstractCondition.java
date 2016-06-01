package com.ilsid.bfa.script;

/**
 * Provides common condition functionality.
 * 
 * @author illia.sydorovych
 *
 */
abstract class AbstractCondition implements Condition {

	private static final Object NULL_VALUE = new NullValue();

	private Object leftValue = NULL_VALUE;

	private Object rightValue = NULL_VALUE;

	private String description;

	/**
	 * Constructs instance with left and right values to check for some condition.
	 * 
	 * @param leftValue
	 * @param rightValue
	 */
	public AbstractCondition(Object leftValue, Object rightValue) {
		if (leftValue != null) {
			this.leftValue = leftValue;
		}
		if (rightValue != null) {
			this.rightValue = rightValue;
		}
	}

	/**
	 * Provides human readable description.
	 * 
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets human readable description.
	 * 
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Provides left value to check for some condition.
	 * 
	 * @return left value
	 */
	protected Object getLeftValue() {
		return leftValue;
	}

	/**
	 * Provides right value to check for some condition.
	 * 
	 * @return right value
	 */
	protected Object getRightValue() {
		return rightValue;
	}

	private static final class NullValue {

		private static final String NULL_VALUE_STR = "Null";

		@Override
		public String toString() {
			return NULL_VALUE_STR;
		}
	}

}
