package com.ilsid.bfa.script;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.ilsid.bfa.action.persistence.ActionLocator;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.runtime.persistence.RuntimeRepository;

/**
 * Provides the script runtime operations.
 * 
 * @author illia.sydorovych
 *
 */
@Singleton
public class ScriptRuntime {

	private static final Object[] EMPTY_PARAMS = new Object[] {};

	private RuntimeRepository repository;

	private ActionLocator actionLocator;

	/**
	 * Runs the script with the given name. The script is searched in the Default Group.
	 * 
	 * @param scriptName
	 *            the script name
	 * @return the script runtime identifier
	 * @throws ScriptException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository within the Default Group</li>
	 *             <li>in case of any repository access issues</li>
	 *             <li>in case of the script runtime failure</li>
	 *             </ul>
	 */
	public long runScript(String scriptName) throws ScriptException {
		return runScript(scriptName, EMPTY_PARAMS);
	}

	/**
	 * Runs the script with the given name and input parameters. The script is searched in the Default Group.
	 * 
	 * @param scriptName
	 *            the script name
	 * @param params
	 *            input parameters
	 * @return the script runtime identifier
	 * @throws ScriptException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository within the Default Group</li>
	 *             <li>in case of any repository access issues</li>
	 *             <li>in case of the script runtime failure</li>
	 *             </ul>
	 */
	public long runScript(String scriptName, Object... params) throws ScriptException {
		Script script = createInstance(scriptName);
		long runtimeId = generatedRuntimeId(scriptName);
		script.setRuntimeId(runtimeId);
		script.setRuntime(this);
		script.setActionLocator(actionLocator);
		script.setInputParameters(params);

		script.execute();

		return runtimeId;
	}

	/**
	 * Defines a code repository implementation.
	 * 
	 * @param repository
	 *            a code repository
	 */
	@Inject
	public void setRepository(RuntimeRepository repository) {
		this.repository = repository;
	}

	/**
	 * Defines the action locator.
	 * 
	 * @param actionLocator
	 *            action locator instance
	 */
	@Inject
	public void setActionLocator(ActionLocator actionLocator) {
		this.actionLocator = actionLocator;
	}

	@SuppressWarnings("unchecked")
	private Script createInstance(String scriptName) throws ScriptException {
		String scriptClassName = TypeNameResolver.resolveScriptClassName(scriptName);
		Class<Script> scriptClass;
		try {
			scriptClass = (Class<Script>) DynamicClassLoader.getInstance().loadClass(scriptClassName);
		} catch (ClassNotFoundException e) {
			throw new ScriptException(String.format("The script [%s] is not found in the repository", scriptName), e);
		} catch (ClassCastException e) {
			throw new ScriptException(
					"Unexpected class was loaded from the repository. Expected: " + Script.class.getName(), e);
		}

		Script script;
		try {
			script = scriptClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ScriptException(String.format("Failed to create an instance of the script [%s]", scriptName), e);
		}

		return script;
	}

	private long generatedRuntimeId(String scriptName) throws ScriptException {
		long runtimeId;
		try {
			runtimeId = repository.getNextRuntimeId();
		} catch (PersistenceException e) {
			throw new ScriptException(String.format("Failed to generate runtime id for the script [%s]", scriptName),
					e);
		}

		return runtimeId;
	}

}
