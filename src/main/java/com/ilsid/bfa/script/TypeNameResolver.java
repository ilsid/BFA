package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.common.GroupNameUtil;

//TODO: write javadocs
public class TypeNameResolver {

	private static final char DOT = '.';

	private static final String EMPTY = "";

	private static final String EXPRESSION_PREFIX = "$$";

	private static final String DOT_STR = ".";

	private static final Map<String, String> predefinedTypes = new HashMap<>();

	static {
		predefinedTypes.put("Number", "java.lang.Integer");
		predefinedTypes.put("Decimal", "java.lang.Double");
	}

	public static String resolveEntityClassName(String entityName) {
		String predefinedType = predefinedTypes.get(entityName);
		if (predefinedType != null) {
			return predefinedType;
		}

		GroupNameUtil.NameParts entityNameParts = splitName(entityName);

		final String simpleClassName = ClassNameUtil.generateSimpleClassName(entityNameParts.getChildName(),
				ClassNameUtil.BLANK_CODE);

		StringBuilder result = new StringBuilder();
		result.append(resolveEntityGroupPackageName(entityNameParts.getParentName())).append(DOT)
				.append(simpleClassName);

		return result.toString();
	}

	public static String resolveScriptClassName(String scriptName) {
		GroupNameUtil.NameParts scriptNameParts = splitName(scriptName);

		final String simpleClassName = ClassNameUtil.generateSimpleClassName(scriptNameParts.getChildName(),
				ClassNameUtil.BLANK_CODE);

		StringBuilder result = new StringBuilder();
		result.append(resolveScriptGroupPackageName(scriptNameParts.getParentName())).append(DOT)
				.append(simpleClassName.toLowerCase()).append(DOT).append(simpleClassName);

		return result.toString();
	}

	public static String resolveExpressionClassName(String scriptName, String expression) {
		StringBuilder result = new StringBuilder();
		result.append(resolveScriptClassName(scriptName)).append(resolveExpressionClassNamePart(expression));

		return result.toString();
	}

	public static String resolveExpressionClassNamePart(String expression) {
		return new StringBuilder(EXPRESSION_PREFIX).append(ClassNameUtil.generateSimpleClassName(expression, EMPTY))
				.toString();
	}

	public static String resolveScriptGroupPackageName(String groupName) {
		return generatePackageName(ClassNameUtil.GENERATED_SCRIPTS_ROOT_PACKAGE, groupName);
	}

	public static String resolveEntityGroupPackageName(String groupName) {
		return generatePackageName(ClassNameUtil.GENERATED_ENTITIES_ROOT_PACKAGE, groupName);
	}

	/**
	 * Splits script or entity name.
	 * 
	 * @param name
	 *            script or entity name.
	 * @return parent and child parts. If no parent is defined then {@link ClassNameUtil#DEFAULT_GROUP_SUBPACKAGE} is
	 *         returned as a parent part
	 */
	public static GroupNameUtil.NameParts splitName(String name) {
		return GroupNameUtil.splitName(name, ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE);
	}

	/**
	 * Splits group name.
	 * 
	 * @param name
	 *            group name.
	 * @return parent and child parts. If no parent is defined then <code>null</code> is returned as a parent part
	 */
	public static GroupNameUtil.NameParts splitGroupName(String name) {
		return GroupNameUtil.splitName(name);
	}

	/*
	 * Generates a full package name for the given parent package and the expression representing a child package. The
	 * expression is treated as a simple group like <i>some_group</i> or a complex group like
	 * <i>grand_parent_group::parent_group::some_group</i>.
	 */
	private static String generatePackageName(String parentPackage, String expression) {
		String childPackage = expression.replaceAll(ClassNameUtil.WHITESPACE_REGEXP, ClassNameUtil.BLANK_CODE);
		childPackage = ClassNameUtil.escape(childPackage);
		childPackage = childPackage.replaceAll(GroupNameUtil.GROUP_SEPARATOR, DOT_STR).toLowerCase();

		StringBuilder result = new StringBuilder();
		result.append(parentPackage).append(DOT).append(childPackage);

		return result.toString();
	}

}
