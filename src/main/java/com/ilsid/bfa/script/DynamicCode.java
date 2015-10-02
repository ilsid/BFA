package com.ilsid.bfa.script;

/**
 * Expression that represents some scripting source code.
 * 
 * @author illia.sydorovych
 *
 */
class DynamicCode implements Expression<Object> {

	private String input;

	private ScriptContext scriptContext;

	private DynamicCodeParser parser;

	private Object value;

	/**
	 * Constructs instance with a specified scripting source code.
	 * 
	 * @param input
	 *            scripting source code
	 * @param scriptContext
	 *            {@link ScriptContext} instance
	 */
	public DynamicCode(String input, ScriptContext scriptContext) {
		this.input = input;
		this.scriptContext = scriptContext;
		parser = new DynamicCodeParser(scriptContext);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ilsid.bfa.script.Expression#getValue()
	 */
	public Object getValue() throws DynamicCodeException {
		if (value != null) {
			return value;
		}

		String javaExpression = parse(input);
		DynamicCodeInvocation invocation = compile(javaExpression);
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
