package com.ilsid.bfa.script;

/**
 * ValueExpression that represents some scripting source code.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptExpression implements ValueExpression<Object> {

	private String input;

	private ScriptContext scriptContext;

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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ilsid.bfa.script.ValueExpression#getValue()
	 */
	public Object getValue() throws ScriptException {
		if (value != null) {
			return value;
		}

		DynamicCodeInvocation invocation;
		try {
			invocation = DynamicCodeFactory.getInvocation(scriptContext.getScriptName(), input,
					new ScriptExpressionParser(scriptContext));
		} catch (DynamicCodeException e) {
			throw new ScriptException("Failed to get a value from the expression [" + input + "]", e);
		}

		invocation.setScriptContext(scriptContext);
		value = invocation.invoke();

		return value;
	}

}
