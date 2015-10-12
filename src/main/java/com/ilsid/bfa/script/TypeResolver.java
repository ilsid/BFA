package com.ilsid.bfa.script;

public interface TypeResolver {
	
	public Action resolveAction(String actionName) throws ScriptException;
	
	public String resolveJavaTypeName(String typeName) throws ScriptException;
	
	public Script resolveSubflow(String flowName) throws ScriptException;

}
