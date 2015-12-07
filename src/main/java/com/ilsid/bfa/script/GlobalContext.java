package com.ilsid.bfa.script;

import com.ilsid.bfa.persistence.ScriptingRepository;

//TODO: Complete implementation
//TODO: Put javadocs
public class GlobalContext {
	
	private static final GlobalContext instance = new GlobalContext();
	
	private ScriptingRepository codeRepository;
	
	GlobalContext() {
	}
	
	public static GlobalContext getInstance() {
		return instance;
	}
	
	public Object getInputVar(String name) {
		return null;
	}
	
	public Object getGlobalVar(String name) {
		return null;
	}
	
	public void setGlobalVar(String name, Object value) {

	}
	
	public ScriptingRepository getCodeRepository() {
		return codeRepository;
	}

	void setCodeRepository(ScriptingRepository codeRepository) {
		this.codeRepository = codeRepository;
	}
	
}
