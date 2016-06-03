package com.ilsid.bfa.action;

/**
 * Action base class.
 * 
 * @author illia.sydorovych
 *
 */
public abstract class Action {

	private static final Object[] EMPTY_PARAMS = new Object[] {};

	private Object[] params = EMPTY_PARAMS;

	/**
	 * Returns a context for this action.
	 * 
	 * @return context
	 */
	protected final ActionContext getContext() {
		return ActionContext.getInstance();
	}

	/**
	 * Returns input parameters for this action.
	 * 
	 * @return input parameters or an empty array if no parameters were passed
	 */
	protected final Object[] getInputParameters() {
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
	 * Defines input parameters for this action.
	 * 
	 * @param params
	 *            action input
	 */
	public final void setInputParameters(Object[] params) {
		if (params != null) {
			this.params = params;
		}
	}
}
