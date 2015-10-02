package com.ilsid.bfa.script;

/**
 * Executable abstraction.
 * 
 * @author illia.sydorovych
 *
 * @param <Result>
 *            execution result
 */
public interface Executable<Result> {

	/**
	 * Triggers execution.
	 * 
	 * @return execution result, if any.
	 * @throws ScriptException
	 *             in case of any failures
	 */
	Result execute() throws ScriptException;

}
