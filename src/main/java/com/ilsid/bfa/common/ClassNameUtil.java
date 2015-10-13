package com.ilsid.bfa.common;

import java.util.HashMap;
import java.util.Map;

public class ClassNameUtil {
	
	public static final String GENERATED_PACKAGE = "com.ilsid.bfa.generated.";
	
	private static Map<String, String> replaceableSymbols = new HashMap<>();

	static {
		replaceableSymbols.put("-", "_Mns_");
		replaceableSymbols.put("+", "_Pls_");
		replaceableSymbols.put("*", "_Mlt_");
		replaceableSymbols.put("/", "_Div_");
		replaceableSymbols.put(".", "%dt");
	}
	
	public static String generateClassName(String scriptName, String expression) {
		return GENERATED_PACKAGE + generateSimpleClassName(scriptName) + "$$" + generateSimpleClassName(expression);
	}

	public static String generateClassName(String scriptName) {
		return GENERATED_PACKAGE + generateSimpleClassName(scriptName);
	}

	private static String generateSimpleClassName(String expression) {
		String expr = expression.replaceAll("\\s", "");
		for (String smb : replaceableSymbols.keySet()) {
			expr = expr.replace(smb, replaceableSymbols.get(smb));
		}

		return expr;
	}
	
}
