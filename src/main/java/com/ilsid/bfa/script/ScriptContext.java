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

	private Map<String, Variable> inputVars = new HashMap<>();

	private Map<String, Variable> localVars = new HashMap<>();

	private String scriptName;

	public void addInputVar(String name, String javaType, Object value) throws ScriptException {
		// FIXME: validate name format
		checkVarNameUniqueness(name);
		Object resolvedValue = resolveValue(javaType, value);
		inputVars.put(name, new Variable(name, javaType, resolvedValue));
	}

	public void addLocalVar(String name, String javaType) throws ScriptException {
		// FIXME: validate name format
		checkVarNameUniqueness(name);
		localVars.put(name, new Variable(name, javaType));
	}

	public void addLocalVar(String name, String javaType, Object initValue) throws ScriptException {
		// FIXME: validate name format
		checkVarNameUniqueness(name);
		Object resolvedValue = resolveValue(javaType, initValue);
		localVars.put(name, new Variable(name, javaType, resolvedValue));
	}

	/**
	 * Returns the local or the input variable by name.
	 * 
	 * @param name
	 *            variable name
	 * @return variable instance or <code>null</code> if variable with such name does not exist
	 */
	public Variable getVar(String name) {
		final Variable localVar = localVars.get(name);
		if (localVar == null) {
			return inputVars.get(name);
		} else {
			return localVar;
		}
	}

	/**
	 * Pre-condition: The name must have the one of two formats: <var name> or <var name>.<field name>
	 * 
	 * @param name
	 * @param value
	 * @throws ScriptException
	 */
	public void updateLocalVar(String name, Object value) throws ScriptException {
		VarNameParts nameParts = getVariableNameParts(name);
		final String varName = nameParts.getVarName();
		final String fieldName = nameParts.getFieldName();
		Variable var = localVars.get(varName);

		if (fieldName == null) {
			Object resolvedValue = resolveValue(var.getJavaType(), value);
			var.setValue(resolvedValue);
		} else {
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

	static interface VarNameParts {

		String getVarName();

		String getFieldName();
	}

	static class VarNamePartsHolder implements VarNameParts {

		String varName;

		String fieldName;

		public String getVarName() {
			return varName;
		}

		public String getFieldName() {
			return fieldName;
		}

	}

	VarNameParts getVariableNameParts(String name) throws ScriptException {
		VarNamePartsHolder parts = new VarNamePartsHolder();
		String[] varParts = name.split("\\.");

		if (varParts.length <= 2) {
			parts.varName = varParts[0];
			checkLocalVarExists(parts.varName);
			
			if (varParts.length == 2) {
				parts.fieldName = varParts[1];
			}
		} else {
			throw new ScriptException("Unexpected variable name format: " + name);
		}

		return parts;
	}

	private void setField(Object target, String fieldName, Object value) throws ScriptException {
		Field field;
		try {
			field = target.getClass().getField(fieldName);
			field.setAccessible(true);
			Object resolvedValue = resolveValue(field.getType().getName(), value);
			field.set(target, resolvedValue);
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
			throw new ScriptException("Local variable with name [" + name + "] has been already declared");
		} else if (inputVars.containsKey(name)) {
			throw new ScriptException("Input variable with name [" + name + "] has been already declared");
		}
	}

	private void checkLocalVarExists(String name) throws ScriptException {
		if (!localVars.containsKey(name)) {
			throw new ScriptException("Local variable with name [" + name + "] is not declared");
		}
	}

	private Object resolveValue(String javaType, Object value) throws ScriptException {
		if (value == null) {
			return null;
		}
		TypeValueResolver resolver = TypeValueResolver.getResolver(javaType);
		return resolver.resolve(value);
	}

}
