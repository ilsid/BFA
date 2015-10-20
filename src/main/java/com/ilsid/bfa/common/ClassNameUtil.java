package com.ilsid.bfa.common;

import java.util.HashMap;
import java.util.Map;

//TODO: write javadocs
public class ClassNameUtil {
	
	private static final String GENERATED_PACKAGE = "com.ilsid.bfa.generated.";
	
	private static final String GENERATED_SCRIPT_PACKAGE = GENERATED_PACKAGE + "script.";
	
	private static final String GENERATED_ACTION_PACKAGE = GENERATED_PACKAGE + "action.";
	
	private static final String GENERATED_POJO_PACKAGE = GENERATED_PACKAGE + "type.";
	
	private static final String EXPRESSION_PREFIX = "$$";

	private static final Map<String, String> replaceableSymbols = new HashMap<>();
	
	private static final Map<String, String> predefinedTypes = new HashMap<>();

	static {
		replaceableSymbols.put("-", "_Mns_");
		replaceableSymbols.put("+", "_Pls_");
		replaceableSymbols.put("*", "_Mlt_");
		replaceableSymbols.put("/", "_Div_");
		replaceableSymbols.put(".", "%dt");
	}
	
	static {
		predefinedTypes.put("Number", "Integer");
		predefinedTypes.put("Decimal", "Double");
	}

	//TODO: write unit tests
	public static String resolveJavaClassName(String scriptName) {
		String predefinedType = predefinedTypes.get(scriptName);
		if (predefinedType != null) {
			return predefinedType;
		}
		
		return GENERATED_POJO_PACKAGE + generateSimpleClassName(scriptName);
	}
	
	public static String resolveScriptClassName(String scriptName) {
		return GENERATED_SCRIPT_PACKAGE + generateSimpleClassName(scriptName);
	}
	
	public static String resolveExpressionClassName(String scriptName, String expression) {
		return GENERATED_SCRIPT_PACKAGE + generateSimpleClassName(scriptName) + EXPRESSION_PREFIX
				+ generateSimpleClassName(expression);
	}
	
	public static String resolveActionClassName(String scriptName) {
		return GENERATED_ACTION_PACKAGE + generateSimpleClassName(scriptName);
	}
	
	private static String generateSimpleClassName(String expression) {
		String expr = expression.replaceAll("\\s", "");
		for (String smb : replaceableSymbols.keySet()) {
			expr = expr.replace(smb, replaceableSymbols.get(smb));
		}

		return expr;
	}

}
