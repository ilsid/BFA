package com.ilsid.bfa.script;

/**
 * Represents boolean expression.
 * 
 * @author illia.sydorovych
 *
 */
class BooleanExpression implements ValueExpression<Boolean> {

	private static final String VALID_TRUE_VALUE = "true";
	
	private static final String VALID_FALSE_VALUE = "false";
	
	private String input;

	/**
	 * Creates instance with specified string representation of boolean value.
	 * The valid values are "true" and "false" (case-insensitive).
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
		if (!VALID_TRUE_VALUE.equalsIgnoreCase(input) && !VALID_FALSE_VALUE.equalsIgnoreCase(input)) {
			throw new ScriptException("Boolean value must be true or false: " + input);
		}
	}

}
