package com.ilsid.bfa.runtime;

import com.ilsid.bfa.persistence.CodeRepository;

//TODO: Complete implementation
//TODO: Put javadocs
public class GlobalContext {
	
	private static final GlobalContext instance = new GlobalContext();
	
	private CodeRepository codeRepository;
	
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
	
	public CodeRepository getCodeRepository() {
		return codeRepository;
	}

	void setCodeRepository(CodeRepository codeRepository) {
		this.codeRepository = codeRepository;
	}
	
}
