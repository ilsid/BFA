package com.ilsid.bfa.script;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.ilsid.bfa.BFAError;

public final class CompilerConstants {
	
	public static final String SCRIPT_CONTEXT_CLASS_NAME = "com.ilsid.bfa.script.ScriptContext";

	public static final String INVOCATION_INTERFACE_NAME = "com.ilsid.bfa.script.DynamicCodeInvocation";

	public static final String SCRIPT_CLASS_NAME = "com.ilsid.bfa.script.Script";
	
	public static final String SCRIPT_IMPLEMENTATION_METHOD_NAME = "doExecute";
	
	public static final String SCRIPT_SOURCE_TEMPLATE;

	public static final Class<?> SCRIPT_CLASS;

	static {
		SCRIPT_CLASS = Script.class;

		try (InputStream tplt = CompilerConstants.class.getResourceAsStream("script_source.tplt")) {
			SCRIPT_SOURCE_TEMPLATE = IOUtils.toString(tplt, "UTF-8");
		} catch (IOException e) {
			throw new BFAError("Failed to load script source template", e);
		}
	}

}
