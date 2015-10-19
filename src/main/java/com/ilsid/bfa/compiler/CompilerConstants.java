package com.ilsid.bfa.compiler;

public final class CompilerConstants {

	public static final String SCRIPT_CONTEXT_CLASS_NAME = "com.ilsid.bfa.script.ScriptContext";

	public static final String INVOCATION_INTERFACE_NAME = "com.ilsid.bfa.script.DynamicCodeInvocation";

	public static final String SCRIPT_CLASS_NAME = "com.ilsid.bfa.script.Script";

	public static final Class<?> SCRIPT_CLASS;

	static {
		try {
			// TODO: custom class loader may be needed here
			SCRIPT_CLASS = Class.forName(CompilerConstants.SCRIPT_CLASS_NAME);
		} catch (ClassNotFoundException e) {
			throw new Error(String.format("Class [%s] not found", CompilerConstants.SCRIPT_CLASS_NAME), e);
		}
	}

}
