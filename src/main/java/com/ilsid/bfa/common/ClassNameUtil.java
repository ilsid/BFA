package com.ilsid.bfa.common;

import java.io.File;

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

	private static final char DOT = '.';

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

}
