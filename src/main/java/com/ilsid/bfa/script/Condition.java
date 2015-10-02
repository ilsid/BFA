package com.ilsid.bfa.script;

/**
 * Logical condition.
 * 
 * @author illia.sydorovych
 *
 */
interface Condition {

	/**
	 * Determines whether the condition is met
	 * 
	 * @return <code>true</code> if the condition is met and <code>false</code>
	 *         otherwise
	 * @throws ScriptException
	 *             in case of any failure
	 */
	boolean isTrue() throws ScriptException;

}
