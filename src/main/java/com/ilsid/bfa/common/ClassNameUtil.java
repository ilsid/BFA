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
	 * The root package for the generated entities.
	 */
	public static final String GENERATED_ENTITIES_ROOT_PACKAGE = GENERATED_CLASSES_PACKAGE + ".entity";

	/**
	 * The default group sub-package for the generated scripts and entities.
	 */
	public static final String DEFAULT_GROUP_SUBPACKAGE = "default_group";

	/**
	 * The default group package for the generated scripts.
	 */
	public static final String GENERATED_SCRIPTS_DEFAULT_GROUP_PACKAGE = GENERATED_SCRIPTS_ROOT_PACKAGE + "."
			+ DEFAULT_GROUP_SUBPACKAGE;

	/**
	 * The default group package for the generated entities.
	 */
	public static final String GENERATED_ENTITIES_DEFAULT_GROUP_PACKAGE = GENERATED_ENTITIES_ROOT_PACKAGE + "."
			+ DEFAULT_GROUP_SUBPACKAGE;

	/**
	 * Meta-data file name.
	 */
	public static final String METADATA_FILE_NAME = "meta.data";

	/**
	 * Blank escape string.
	 */
	public static final String BLANK_CODE = "_x20_";

	/**
	 * Whitespace regular expression.
	 */
	public static final String WHITESPACE_REGEXP = "\\s";

	/**
	 * Class file extension.
	 */
	public static final String CLASS_FILE_EXTENSION = ".class";

	private static final char DOT = '.';

	private static final Map<String, String> escapeSymbols;

	static {
		escapeSymbols = new HashMap<>();

		escapeSymbols.put("+", "_Pls_");
		escapeSymbols.put("-", "_Mns_");
		escapeSymbols.put("*", "_Mlt_");
		escapeSymbols.put("/", "_Div_");
		escapeSymbols.put(".", "_dt_");
		escapeSymbols.put("'", "_sq_");
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
		String expr = escape(expression.replaceAll(WHITESPACE_REGEXP, blankReplacement));
		return expr;
	}

	/**
	 * Escapes the given expression.
	 * 
	 * @param expression
	 *            expression to escape
	 * @return the expression with the following escaped symbols: [+-*./]
	 */
	public static String escape(String expression) {
		String result = expression;
		for (String smb : escapeSymbols.keySet()) {
			result = result.replace(smb, escapeSymbols.get(smb));
		}

		return result;
	}

	/**
	 * Returns path for the given class name. For example, the method returns the string
	 * <i>com/foo/bar/MyClass.class</i> for the class name <i>com.foo.bar.MyClass</i>.
	 * 
	 * @param className
	 *            a fully qualified class name
	 * @return path to the class
	 */
	public static String getPath(String className) {
		return className.replace(DOT, File.separatorChar).concat(CLASS_FILE_EXTENSION);
	}

}
