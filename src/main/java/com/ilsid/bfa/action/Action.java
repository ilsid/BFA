package com.ilsid.bfa.action;

/**
 * Action base class.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class Action {
	
	private Object[] params; 
	
	protected Object[] getInputParameters() {
		return params;
	}

	/**
	 * Executes an action.
	 * 
	 * @return an execution result or an empty array, if an action is not supposed to provide any result
	 * @throws ActionException
	 *             in case of any failures
	 */
	public abstract Object[] execute() throws ActionException;

	/**
	 * Defines input parameters for this action, if any.
	 * 
	 * @param params
	 *            action input
	 */
	public void setInputParameters(Object[] params) {
		this.params = params;
	}

}
