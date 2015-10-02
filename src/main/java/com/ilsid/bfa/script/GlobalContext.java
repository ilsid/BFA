package com.ilsid.bfa.script;

//TODO: Complete implementation
//TODO: Put javadocs
class GlobalContext {
	
	
	public Object getInputVar(String name) {
		return null;
	}
	
	public Object getGlobalVar(String name) {
		return null;
	}
	
	public void setGlobalVar(String name, Object value) {

	}
	
	public TypeResolver getTypeResolver() {
		return new TypeResolver() {
			
			public String resolveJavaTypeName(String typeName) throws ScriptException {
				return typeName;
			}
			
			public Action resolveAction(String actionName) throws ScriptException {
				return null;
			}
		};
	}

}
