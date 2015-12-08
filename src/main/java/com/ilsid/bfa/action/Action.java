package com.ilsid.bfa.action;

/**
 * Action abstraction.
 * 
 * @author illia.sydorovych
 *
 */
public interface Action {

	/**
	 * Executes an action.
	 * 
	 * @return an execution result or an empty array, if an action is not supposed to provide any result
	 * @throws ActionException
	 *             in case of any failures
	 */
	Object[] execute() throws ActionException;

	/**
	 * Defines input parameters for this action, if any.
	 * 
	 * @param params
	 *            action input
	 */
	void setInputParameters(Object[] params);

}
