package com.ilsid.bfa.script;

import javax.inject.Inject;

import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.CodeRepository;
import com.ilsid.bfa.persistence.PersistenceException;

/**
 * Provides the script runtime operations.
 * 
 * @author illia.sydorovych
 *
 */
// TODO: support for group name and input params is needed
public class ScriptRuntime {

	private CodeRepository repository;

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
	 *             </ul>
	 */
	public long runScript(String scriptName) throws ScriptException {
		Script script = createInstance(scriptName);
		long runtimeId = generatedRuntimeId(scriptName);
		script.setRuntimeId(runtimeId);
		script.setRuntime(this);
		
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
	public void setRepository(CodeRepository repository) {
		this.repository = repository;
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
