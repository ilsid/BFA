package com.ilsid.bfa.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the routines for the class names processing.
 * 
 * @author illia.sydorovych
 *
 */
public class ClassNameUtil {

	/**
	 * The root package for all generated classes.
	 */
	public static final String GENERATED_CLASSES_PACKAGE = "com.ilsid.bfa.generated";
	
	/**
	 * The root package for the generated scripts.
	 */
	public static final String GENERATED_SCRIPTS_ROOT_PACKAGE = GENERATED_CLASSES_PACKAGE + ".script";

	/**
	 * The default group package for the generated scripts.
	 */
	public static final String GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE = GENERATED_SCRIPTS_ROOT_PACKAGE + ".default_group";

	public static final String BLANK_CODE = "_x20_";

	private static final char DOT = '.';

	private static final Map<String, String> replaceableSymbols = new HashMap<>();

	static {
		replaceableSymbols.put("-", "_Mns_");
		replaceableSymbols.put("+", "_Pls_");
		replaceableSymbols.put("*", "_Mlt_");
		replaceableSymbols.put("/", "_Div_");
		replaceableSymbols.put(".", "_dt_");
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

	/**
	 * Return a class name without package.
	 * 
	 * @param className
	 *            a class name including package
	 * @return a class name without package
	 */
	public static String getShortClassName(String className) {
		int lastDotIdx = className.lastIndexOf('.');
		return className.substring(lastDotIdx + 1);
	}

	/**
	 * Returns a package name for the given class name.
	 * 
	 * @param className
	 *            a class name
	 * @return a package name
	 */
	public static String getPackageName(String className) {
		int lastDotIdx = className.lastIndexOf('.');
		return className.substring(0, lastDotIdx);
	}

	/**
	 * Generates a simple class name (without a package) for the given expression.
	 * 
	 * @param expression
	 *            the expression to generate a class name
	 * @param blankReplacement
	 *            a string that replaces blank symbols in the given expression
	 * @return a class name
	 */
	public static String generateSimpleClassName(String expression, String blankReplacement) {
		String expr = expression.replaceAll("\\s", blankReplacement);
		for (String smb : replaceableSymbols.keySet()) {
			expr = expr.replace(smb, replaceableSymbols.get(smb));
		}

		return expr;
	}

}
