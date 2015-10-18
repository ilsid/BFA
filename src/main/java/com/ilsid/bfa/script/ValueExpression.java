package com.ilsid.bfa.script;

/**
 * Expression that provides some value.
 * 
 * @author illia.sydorovych
 *
 * @param <T>
 */
interface ValueExpression<T> {

	/**
	 * Provides calculation result.
	 * 
	 * @return result
	 * @throws ScriptException
	 *             in case of any failures
	 */
	T getValue() throws ScriptException;

}
