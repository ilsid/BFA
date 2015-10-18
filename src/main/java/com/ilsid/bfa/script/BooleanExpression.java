package com.ilsid.bfa.script;

import org.apache.commons.lang3.Validate;

/**
 * Represents boolean expression.
 * 
 * @author illia.sydorovych
 *
 */
class BooleanExpression implements ValueExpression<Boolean> {

	private static final String VALID_TRUE_VALUE = "True";
	
	private static final String VALID_FALSE_VALUE = "False";
	
	private String input;

	/**
	 * Creates instance with specified string representation of boolean value.
	 * The valid values are "True" and "False" (case-sensitive).
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
	 *             if the passed value is not "True" or "False".
	 * @see {@link BooleanExpression#BooleanExpression(String)}
	 */
	@Override
	public Boolean getValue() throws ScriptException {
		validateInput(input);
		return Boolean.valueOf(input);
	}

	private void validateInput(String input) throws ScriptException {
		if (input == null) {
			throw new IllegalArgumentException("Input is null");
		}

		Validate.notEmpty(input, "Input is empty");

		if (!input.equals(VALID_TRUE_VALUE) && !input.equals(VALID_FALSE_VALUE)) {
			throw new ScriptException("Boolean value must be True or False: " + input);
		}
	}

}
