package com.ilsid.bfa.script;

/**
 * Expression that provides some value as a result of its calculation.
 * 
 * @author illia.sydorovych
 *
 * @param <T>
 */
interface Expression<T> {

	/**
	 * Provides calculation result.
	 * 
	 * @return result
	 * @throws ScriptException
	 *             in case of any failures
	 */
	T getValue() throws ScriptException;

}
