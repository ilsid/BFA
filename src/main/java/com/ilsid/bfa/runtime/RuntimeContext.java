package com.ilsid.bfa.runtime;

import com.ilsid.bfa.persistence.CodeRepository;

//TODO: Complete implementation
//TODO: Put javadocs
public class RuntimeContext {
	
	private static final RuntimeContext instance = new RuntimeContext();
	
	private CodeRepository codeRepository;
	
	RuntimeContext() {
	}
	
	public static RuntimeContext getInstance() {
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

	public void setCodeRepository(CodeRepository codeRepository) {
		this.codeRepository = codeRepository;
	}
	
}
