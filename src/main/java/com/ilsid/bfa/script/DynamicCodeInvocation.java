package com.ilsid.bfa.script;

/**
 * Invokes dynamic Java code obtained from scripting expression.
 * 
 * @author illia.sydorovych
 *
 */
public interface DynamicCodeInvocation {

	/**
	 * Invokes dynamic Java expression.
	 * 
	 * @return the expression result
	 */
	Object invoke();

	/**
	 * Defines script context.
	 * 
	 * @param scriptContext
	 *            the context
	 */
	void setScriptContext(ScriptContext scriptContext);

}
