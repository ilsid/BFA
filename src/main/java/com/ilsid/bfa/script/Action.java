package com.ilsid.bfa.script;

/**
 * Action abstraction.
 * 
 * @author illia.sydorovych
 *
 */
interface Action extends Executable<Object[]> {

	/**
	 * Defines input parameters for this action, if any.
	 * 
	 * @param params
	 *            action input
	 */
	void setInputParameters(Object[] params);

}
