package com.ilsid.bfa.script;

import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.ilsid.bfa.action.persistence.ActionLocator;
import com.ilsid.bfa.persistence.DynamicClassLoader;
import com.ilsid.bfa.persistence.PersistenceException;
import com.ilsid.bfa.runtime.dto.RuntimeStatusType;
import com.ilsid.bfa.runtime.dto.ScriptRuntimeDTO;
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
	 * Runs the script with the given name.
	 * 
	 * @param scriptName
	 *            the script name
	 * @return the script runtime identifier
	 * @throws ScriptException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             <li>in case of the script runtime failure</li>
	 *             </ul>
	 */
	public long runScript(String scriptName) throws ScriptException {
		return runScript(scriptName, EMPTY_PARAMS, null, null);
	}

	/**
	 * Runs the script with the given name and input parameters.
	 * 
	 * @param scriptName
	 *            the script name
	 * @param params
	 *            input parameters
	 * @return the script runtime identifier
	 * @throws ScriptException
	 *             <ul>
	 *             <li>if the script with such name does not exist in the repository</li>
	 *             <li>in case of any repository access issues</li>
	 *             <li>in case of the script runtime failure</li>
	 *             </ul>
	 */
	public long runScript(String scriptName, Object[] params) throws ScriptException {
		return runScript(scriptName, params, null, null);
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

	long runScript(String scriptName, long runtimeId, Deque<String> callStack) throws ScriptException {
		return runScript(scriptName, EMPTY_PARAMS, runtimeId, callStack);
	}

	long runScript(String scriptName, Object[] params, Long runtimeId, Deque<String> callStack) throws ScriptException {
		Script script = createInstance(scriptName);

		long flowRuntimeId;
		if (runtimeId == null) {
			flowRuntimeId = generatedRuntimeId(scriptName);
		} else {
			flowRuntimeId = runtimeId;
		}

		script.setRuntimeId(flowRuntimeId);
		script.setName(scriptName);
		script.setRuntime(this);
		script.setActionLocator(actionLocator);
		script.setInputParameters(params);
		if (callStack != null) {
			script.setCallStack(callStack);
		}

		ScriptRuntimeDTO newRecord = new ScriptRuntimeDTO().setRuntimeId(flowRuntimeId).setScriptName(scriptName)
				.setParameters(toStrings(params)).setStatus(RuntimeStatusType.INPROGRESS).setStartTime(new Date())
				.setCallStack(callStack);

		createRuntimeRecord(newRecord);
		try {
			script.execute();
		} catch (ScriptException | RuntimeException e) {
			updateRuntimeRecord(new ScriptRuntimeDTO().setRuntimeId(flowRuntimeId).setScriptName(scriptName)
					.setStatus(RuntimeStatusType.FAILED).setError(e).setEndTime(new Date()));
			throw e;
		}
		updateRuntimeRecord(new ScriptRuntimeDTO().setRuntimeId(flowRuntimeId).setScriptName(scriptName)
				.setStatus(RuntimeStatusType.COMPLETED).setEndTime(new Date()));

		return flowRuntimeId;
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

	private void createRuntimeRecord(ScriptRuntimeDTO record) throws ScriptException {
		try {
			repository.createRuntimeRecord(record);
		} catch (PersistenceException e) {
			throw new ScriptException(
					String.format("Failed to create runtime record for the script [%s]", record.getScriptName()), e);
		}

	}

	private void updateRuntimeRecord(ScriptRuntimeDTO record) throws ScriptException {
		try {
			repository.updateRuntimeRecord(record);
		} catch (PersistenceException e) {
			throw new ScriptException(
					String.format("Failed to update runtime record for the script [%s]", record.getScriptName()), e);
		}

	}

	private List<String> toStrings(Object[] params) {
		List<String> result = new LinkedList<>();
		for (Object param : params) {
			result.add(param.toString());
		}

		return result;
	}
}
