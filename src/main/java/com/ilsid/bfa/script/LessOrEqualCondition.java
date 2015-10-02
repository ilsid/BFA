package com.ilsid.bfa.script;

/**
 * Returns <code>true</code> if left value is less or equal right value and
 * <code>false</code> otherwise. The left and right values must implement
 * {@link Comparable} interface.
 * 
 * @author illia.sydorovych
 *
 */
public class LessOrEqualCondition extends AbstractCondition {

	/**
	 * 
	 * @param leftValue
	 * @param rightValue
	 */
	public LessOrEqualCondition(Object leftValue, Object rightValue) {
		super(leftValue, rightValue);
	}

	/**
	 * See {@link LessOrEqualCondition} description.
	 * 
	 * @throws ScriptException
	 *             if not both left and right values implement
	 *             {@link Comparable} interface
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean isTrue() throws ScriptException {
		Comparable leftValue = toComparable(getLeftValue());
		Comparable rightValue = toComparable(getRightValue());

		return leftValue.compareTo(rightValue) <= 0;
	}

	@SuppressWarnings("rawtypes")
	private Comparable toComparable(Object value) throws ScriptException {
		Comparable cmp;
		try {
			cmp = (Comparable) value;
		} catch (ClassCastException e) {
			throw new ScriptException("Value [" + value + "] can not be compared. Type: " + value.getClass().getName());
		}

		return cmp;
	}

}
