package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.runtime.RuntimeContext;

/**
 * Script context. Holds state for a single script.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptContext {

	private RuntimeContext runtimeContext;

	private Map<String, Variable> inputVars = new HashMap<>();

	private Map<String, Variable> localVars = new HashMap<>();

	private String scriptName;
	
	public ScriptContext() {
	}

	/**
	 * Creates new instance.
	 * 
	 * @param runtimeContext
	 *            a runtime context shared between all scripts.
	 */
	public ScriptContext(RuntimeContext runtimeContext) {
		this.runtimeContext = runtimeContext;
	}
	
	/**
	 * Provides runtime context.
	 * @return runtime context
	 */
	public RuntimeContext getRuntimeContext() {
		return runtimeContext;
	}

	public void addInputVar(String name, String javaType) throws ScriptException {
		// FIXME: validate name format
		checkVarNameUniqueness(name);
		inputVars.put(name, new Variable(name, javaType, runtimeContext.getInputVar(name)));
	}

	public void addLocalVar(String name, String javaType) throws ScriptException {
		// FIXME: validate name format
		checkVarNameUniqueness(name);
		localVars.put(name, new Variable(name, javaType));
	}

	public void addLocalVar(String name, String javaType, Object initValue) throws ScriptException {
		// FIXME: validate name format
		// FIXME: validate value type
		checkVarNameUniqueness(name);
		localVars.put(name, new Variable(name, javaType, initValue));
	}

	/**
	 * Returns local variable by name.
	 * 
	 * @param name
	 *            variable name
	 * @return variable instance or <code>null</code> if variable with such name
	 *         does not exist
	 */
	public Variable getLocalVar(String name) {
		return localVars.get(name);
	}

	public void updateLocalVar(String name, Object value) throws ScriptException {
		// FIXME: validate name format
		// FIXME: validate value type
		checkLocalVarExists(name);
		Variable var = localVars.get(name);
		var.setValue(value);
	}

	public String getScriptName() {
		return scriptName;
	}

	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}

	private void checkVarNameUniqueness(String name) throws ScriptException {
		if (localVars.containsKey(name)) {
			throw new ScriptException("Local variable with name [" + name + "] is already declared");
		} else if (inputVars.containsKey(name)) {
			throw new ScriptException("Input variable with name [" + name + "] is already declared");
		}
	}

	private void checkLocalVarExists(String name) throws ScriptException {
		if (!localVars.containsKey(name)) {
			throw new ScriptException("Local variable with name [" + name + "] is not declared");
		}
	}

}
