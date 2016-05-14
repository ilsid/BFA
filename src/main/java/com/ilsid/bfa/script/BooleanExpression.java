package com.ilsid.bfa.script;

import com.ilsid.bfa.common.BooleanUtil;

/**
 * Represents boolean expression.
 * 
 * @author illia.sydorovych
 *
 */
class BooleanExpression implements ValueExpression<Boolean> {

	private String input;

	/**
	 * Creates instance with specified string representation of boolean value. The valid values are "true" and "false"
	 * (case-insensitive).
	 * 
	 * @param input
	 */
	public BooleanExpression(String input) {
		this.input = input;
	}

	/**
	 * Returns corresponding {@link Boolean} instance.
	 * 
	 * @throws ScriptException
	 *             if the passed value is not "true" or "false" (case-insensitive).
	 */
	@Override
	public Boolean getValue() throws ScriptException {
		validateInput(input);
		return Boolean.valueOf(input);
	}

	private void validateInput(String input) throws ScriptException {
		if (!BooleanUtil.isBoolean(input)) {
			throw new ScriptException("Boolean value must be true or false: " + input);
		}
	}

}
