package com.ilsid.bfa.script;

import javax.inject.Inject;

import com.ilsid.bfa.persistence.CodeRepository;

//TODO: support for group name and input params is needed
public class ScriptRuntime {
	
	private CodeRepository repository;
	
	public void runScript(String scriptName) throws ScriptException {
		Script script;
		try {
			script = DynamicCodeFactory.getScript(scriptName, null);
		} catch (DynamicCodeException e) {
			throw new ScriptException(String.format("Failed to initialize the script [%s]", scriptName), e);
		}
		
		script.execute();
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

}
