package com.ilsid.bfa.script;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

//TODO: write javadocs
public class ClassNameUtil {

	private static final String GENERATED_ROOT_PACKAGE = "com.ilsid.bfa.generated.";

	private static final String GENERATED_SCRIPT_ROOT_PACKAGE = GENERATED_ROOT_PACKAGE + "script.";

	private static final String GENERATED_ACTION_PACKAGE = GENERATED_ROOT_PACKAGE + "action.";

	private static final String GENERATED_POJO_PACKAGE = GENERATED_ROOT_PACKAGE + "type.";

	private static final String EXPRESSION_PREFIX = "$$";

	private static final char DOT = '.';

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

	public static String resolveJavaClassName(String typeName) {
		String predefinedType = predefinedTypes.get(typeName);
		if (predefinedType != null) {
			return predefinedType;
		}

		return GENERATED_POJO_PACKAGE + typeName;
	}

	public static String resolveScriptClassName(String scriptName) {
		final String simpleClassName = generateSimpleClassName(scriptName);
		return generateScriptPackageName(simpleClassName) + simpleClassName;
	}

	public static String resolveExpressionClassName(String scriptName, String expression) {
		final String scriptSimpleClassName = generateSimpleClassName(scriptName);
		return generateScriptPackageName(scriptSimpleClassName) + scriptSimpleClassName + EXPRESSION_PREFIX
				+ generateSimpleClassName(expression);
	}

	public static String resolveActionClassName(String actionName) {
		return GENERATED_ACTION_PACKAGE + generateSimpleClassName(actionName);
	}

	public static String getShortClassName(String className) {
		int lastDotIdx = className.lastIndexOf('.');
		return className.substring(lastDotIdx + 1);
	}

	/**
	 * Returns directories string from the given class name. For example, the method returns the string
	 * <i>com/foo/bar</i> for the class name <i>com.foo.bar.MyClass</i>.
	 * 
	 * @param className
	 *            a fully qualified class name
	 * @return directories for the class' package
	 */
	public static String getDirs(String className) {
		int lastDotIdx = className.lastIndexOf(DOT);
		return className.substring(0, lastDotIdx).replace(DOT, File.separatorChar);
	}

	private static String generateSimpleClassName(String expression) {
		String expr = expression.replaceAll("\\s", "");
		for (String smb : replaceableSymbols.keySet()) {
			expr = expr.replace(smb, replaceableSymbols.get(smb));
		}

		return expr;
	}

	private static String generateScriptPackageName(String simpleScriptClassName) {
		return GENERATED_SCRIPT_ROOT_PACKAGE + simpleScriptClassName.toLowerCase() + DOT;
	}

}
