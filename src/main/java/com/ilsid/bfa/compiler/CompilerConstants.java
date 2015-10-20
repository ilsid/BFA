package com.ilsid.bfa.compiler;

import com.ilsid.bfa.BFAError;
import com.ilsid.bfa.common.BFAClassLoader;

public final class CompilerConstants {

	public static final String SCRIPT_CONTEXT_CLASS_NAME = "com.ilsid.bfa.script.ScriptContext";

	public static final String INVOCATION_INTERFACE_NAME = "com.ilsid.bfa.script.DynamicCodeInvocation";

	public static final String SCRIPT_CLASS_NAME = "com.ilsid.bfa.script.Script";

	public static final Class<?> SCRIPT_CLASS;

	static {
		try {
			// TODO: test custom class loader after implementation completion
			SCRIPT_CLASS = Class.forName(CompilerConstants.SCRIPT_CLASS_NAME, true, BFAClassLoader.getInstance());
		} catch (ClassNotFoundException e) {
			throw new BFAError(String.format("Class [%s] not found", CompilerConstants.SCRIPT_CLASS_NAME), e);
		}
	}

}
