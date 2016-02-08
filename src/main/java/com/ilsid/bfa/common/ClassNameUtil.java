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
	 * The default group sub-package for the generated scripts.
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
	 * The separator used for sub-group naming. For example, the group name can be
	 * <i>grand_parent_group::parent_group::group</i>.
	 */
	public static final String GROUP_SEPARATOR = "::";

	private static final char DOT = '.';

	private static final String DOT_STR = ".";

	private static final Map<String, String> replaceableSymbols;

	static {
		replaceableSymbols = new HashMap<>();

		replaceableSymbols.put("+", "_Pls_");
		replaceableSymbols.put("-", "_Mns_");
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

	/**
	 * Generates a full package name for the given parent package and the expression representing a child package. The
	 * expression is treated as a simple group like <i>some_group</i> or a complex group like
	 * <i>grand_parent_group::parent_group::some_group</i>.
	 * 
	 * @param parentPackage
	 *            parent package
	 * @param expression
	 *            the expression to generate a full package name
	 * @return a full package name
	 */
	public static String generatePackageName(String parentPackage, String expression) {
		String childPackage = expression.replaceAll("\\s", BLANK_CODE);
		for (String smb : replaceableSymbols.keySet()) {
			childPackage = childPackage.replace(smb, replaceableSymbols.get(smb));
		}
		childPackage = childPackage.replaceAll(GROUP_SEPARATOR, DOT_STR).toLowerCase();

		StringBuilder result = new StringBuilder();
		result.append(parentPackage).append(DOT).append(childPackage);

		return result.toString();
	}

}
