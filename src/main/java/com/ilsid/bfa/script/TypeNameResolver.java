package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

//TODO: write javadocs
public class TypeNameResolver {
	
	/**
	 * The root package for all generated classes.
	 */
	public static final String GENERATED_CLASSES_PACKAGE = "com.ilsid.bfa.generated";

	private static final char DOT = '.';
	
	private static final String GENERATED_ROOT_PACKAGE = GENERATED_CLASSES_PACKAGE + DOT;

	private static final String GENERATED_SCRIPT_ROOT_PACKAGE = GENERATED_ROOT_PACKAGE + "script.";

	private static final String GENERATED_SCRIPT_DEFAULT_GROUP_PACKAGE = GENERATED_SCRIPT_ROOT_PACKAGE
			+ "default_group.";
	
	private static final String EMPTY = "";
	
	private static final String BLANK_CODE = "_x20_";

	private static final String GENERATED_ACTION_PACKAGE = GENERATED_ROOT_PACKAGE + "action.";

	private static final String GENERATED_ENTITY_PACKAGE = GENERATED_ROOT_PACKAGE + "entity.";

	private static final String EXPRESSION_PREFIX = "$$";

	private static final Map<String, String> replaceableSymbols = new HashMap<>();

	private static final Map<String, String> predefinedTypes = new HashMap<>();

	static {
		replaceableSymbols.put("-", "_Mns_");
		replaceableSymbols.put("+", "_Pls_");
		replaceableSymbols.put("*", "_Mlt_");
		replaceableSymbols.put("/", "_Div_");
		replaceableSymbols.put(".", "_dt_");
	}

	static {
		predefinedTypes.put("Number", "Integer");
		predefinedTypes.put("Decimal", "Double");
	}

	public static String resolveEnityClassName(String typeName) {
		String predefinedType = predefinedTypes.get(typeName);
		if (predefinedType != null) {
			return predefinedType;
		}

		return GENERATED_ENTITY_PACKAGE + typeName;
	}

	public static String resolveScriptClassName(String scriptName) {
		final String simpleClassName = generateSimpleClassName(scriptName, BLANK_CODE);
		return generateScriptPackageName(simpleClassName) + simpleClassName;
	}

	public static String resolveExpressionClassName(String scriptName, String expression) {
		final String scriptSimpleClassName = generateSimpleClassName(scriptName, BLANK_CODE);
		return generateScriptPackageName(scriptSimpleClassName) + scriptSimpleClassName + EXPRESSION_PREFIX
				+ generateSimpleClassName(expression, EMPTY);
	}

	public static String resolveActionClassName(String actionName) {
		return GENERATED_ACTION_PACKAGE + generateSimpleClassName(actionName, BLANK_CODE);
	}

	private static String generateSimpleClassName(String expression, String blankReplacement) {
		String expr = expression.replaceAll("\\s", blankReplacement);
		for (String smb : replaceableSymbols.keySet()) {
			expr = expr.replace(smb, replaceableSymbols.get(smb));
		}

		return expr;
	}

	private static String generateScriptPackageName(String simpleScriptClassName) {
		return GENERATED_SCRIPT_DEFAULT_GROUP_PACKAGE + simpleScriptClassName.toLowerCase() + DOT;
	}

}
