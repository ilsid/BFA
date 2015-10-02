package com.ilsid.bfa.script;

/**
 * Returns <code>true</code> if left value is equal right value and
 * <code>false</code> otherwise.
 * 
 * @author illia.sydorovych
 *
 */
public class EqualCondition extends AbstractCondition {
	
	/**
	 * 
	 * @param leftvalue
	 * @param rightValue
	 */
	public EqualCondition(Object leftvalue, Object rightValue) {
		super(leftvalue, rightValue);
	}

	/**
	 * See {@link EqualCondition} description.
	 */
	public boolean isTrue() throws ScriptException {
		return getLeftValue().equals(getRightValue());
	}

}
