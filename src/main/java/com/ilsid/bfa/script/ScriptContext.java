package com.ilsid.bfa.script;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.persistence.DynamicClassLoader;

/**
 * Script context. Holds state for a single script.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: complete javadocs
public class ScriptContext {

	private GlobalContext runtimeContext;

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
	public ScriptContext(GlobalContext runtimeContext) {
		this.runtimeContext = runtimeContext;
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
	 * @return variable instance or <code>null</code> if variable with such name does not exist
	 */
	public Variable getLocalVar(String name) {
		return localVars.get(name);
	}

	/**
	 * Pre-condition: The name must have the one of two formats: <var name> or <var name>.<field name>
	 * 
	 * @param name
	 * @param value
	 * @throws ScriptException
	 */
	public void updateLocalVar(String name, Object value) throws ScriptException {
		String[] varParts = name.split("\\.");
		if (varParts.length > 2) {
			throw new IllegalArgumentException("Unexpected variable name format: " + name);
		} else if (varParts.length == 1) {
			checkLocalVarExists(name);
			Variable var = localVars.get(name);
			var.setValue(value);
		} else {
			String varName = varParts[0];
			String fieldName = varParts[1];

			checkLocalVarExists(varName);
			Variable var = localVars.get(varName);
			Object varTarget = var.getValue();
			if (varTarget == null) {
				varTarget = createInstance(var);
				var.setValue(varTarget);
			}
			setField(varTarget, fieldName, value);
		}

	}

	public String getScriptName() {
		return scriptName;
	}

	public void setScriptName(String scriptName) {
		this.scriptName = scriptName;
	}

	private void setField(Object target, String fieldName, Object value) {
		Field field;
		try {
			field = target.getClass().getField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException(
					String.format("Failed to set the field [%s] for [%s] instance", fieldName, target.getClass()), e);
		}
	}

	private Object createInstance(Variable var) {
		Object result;
		String className = var.getJavaType();
		try {
			result = DynamicClassLoader.getInstance().loadClass(className).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalStateException e) {
			throw new IllegalStateException(String.format("Failed to create an instance of [%s]", className), e);
		}

		return result;
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
