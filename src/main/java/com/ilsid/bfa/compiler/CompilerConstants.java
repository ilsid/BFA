package com.ilsid.bfa.compiler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.BFAError;
import com.ilsid.bfa.common.BFAClassLoader;

public final class CompilerConstants {
	
	public static final String SCRIPT_CONTEXT_CLASS_NAME = "com.ilsid.bfa.script.ScriptContext";

	public static final String INVOCATION_INTERFACE_NAME = "com.ilsid.bfa.script.DynamicCodeInvocation";

	public static final String SCRIPT_CLASS_NAME = "com.ilsid.bfa.script.Script";
	
	public static final String SCRIPT_SOURCE_TEMPLATE;

	public static final Class<?> SCRIPT_CLASS;

	static {
		try {
			// TODO: test custom class loader after implementation completion
			SCRIPT_CLASS = Class.forName(CompilerConstants.SCRIPT_CLASS_NAME, true, BFAClassLoader.getInstance());
		} catch (ClassNotFoundException e) {
			throw new BFAError(String.format("Class [%s] not found", CompilerConstants.SCRIPT_CLASS_NAME), e);
		}
		
		try (InputStream tplt = CompilerConstants.class.getResourceAsStream("script_source.tplt")) {
			SCRIPT_SOURCE_TEMPLATE = IOUtils.toString(tplt, "UTF-8");
		} catch (IOException e) {
			throw new BFAError("Failed to load script source template", e);
		}
	}

}
