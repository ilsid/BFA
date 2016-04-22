package com.ilsid.bfa.script;

import com.ilsid.bfa.persistence.DynamicClassLoader;

/**
 * ValueExpression that represents some scripting source code.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptExpression implements ValueExpression<Object> {

	private static final String ERROR_MSG_TEMPLATE = "Failed to get a value from the expression [%s]";

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

		String invocationClassName = TypeNameResolver.resolveExpressionClassName(scriptContext.getScriptName(), input);
		DynamicCodeInvocation invocation;
		try {
			Class<?> invocationClazz = DynamicClassLoader.getInstance().loadClass(invocationClassName);
			invocation = (DynamicCodeInvocation) invocationClazz.newInstance();
		} catch (ClassNotFoundException | IllegalStateException | InstantiationException | IllegalAccessException
				| ClassCastException e) {
			throw new ScriptException(String.format(ERROR_MSG_TEMPLATE, input), e);
		}

		invocation.setScriptContext(scriptContext);
		try {
			value = invocation.invoke();
		} catch (RuntimeException e) {
			throw new ScriptException(String.format(ERROR_MSG_TEMPLATE, input), e);
		}

		return value;
	}

}
