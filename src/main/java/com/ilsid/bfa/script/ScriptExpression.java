package com.ilsid.bfa.script;

/**
 * Expression that represents some scripting source code.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptExpression implements Expression<Object> {

	private String input;

	private ScriptContext scriptContext;

	private ScriptExpressionParser parser;

	private Object value;

	/**
	 * Constructs instance with a specified scripting source code.
	 * 
	 * @param input
	 *            scripting source code
	 * @param scriptContext
	 *            {@link ScriptContext} instance
	 */
	public ScriptExpression(String input, ScriptContext scriptContext) {
		this.input = input;
		this.scriptContext = scriptContext;
		parser = new ScriptExpressionParser(scriptContext);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.script.Expression#getValue()
	 */
	public Object getValue() throws ScriptException {
		if (value != null) {
			return value;
		}

		String javaExpression;
		DynamicCodeInvocation invocation;
		try {
			javaExpression = parse(input);
			invocation = compile(javaExpression);
		} catch (DynamicCodeException e) {
			throw new ScriptException("Failed to get a value from the expression [" + input + "]", e);
		}

		invocation.setScriptContext(scriptContext);
		value = invocation.invoke();

		return value;
	}

	private String parse(String input) throws DynamicCodeException {
		String result = parser.parse(input);
		return result;
	}

	private DynamicCodeInvocation compile(String javaExpression) throws DynamicCodeException {
		DynamicCodeInvocation result = DynamicCodeFactory.getInvocation(scriptContext.getScriptName(), input,
				javaExpression);
		return result;
	}

}
