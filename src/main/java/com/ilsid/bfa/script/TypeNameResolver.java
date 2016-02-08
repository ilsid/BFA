package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.common.ClassNameUtil;

//TODO: write javadocs
public class TypeNameResolver {

	private static final char DOT = '.';

	private static final String EMPTY = "";

	private static final String GENERATED_ENTITY_ROOT_PACKAGE = ClassNameUtil.GENERATED_ENTITIES_ROOT_PACKAGE + DOT;

	private static final String GENERATED_ENTITY_DEFAULT_GROUP_PACKAGE = ClassNameUtil.GENERATED_ENTITIES_DEFAULT_GROUP_PACKAGE
			+ DOT;

	private static final String EXPRESSION_PREFIX = "$$";

	/*
	 * The separator used for sub-group naming. For example, the group name can be
	 * <i>grand_parent_group::parent_group::group</i>.
	 */
	private static final String GROUP_SEPARATOR = "::";

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

		return GENERATED_ENTITY_DEFAULT_GROUP_PACKAGE + entityName;
	}

	public static String resolveScriptClassName(String scriptName) {
		NameParts scriptNameParts = splitName(scriptName);

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

	public static NameParts splitName(String name) {
		int childSepIdx = name.lastIndexOf(ClassNameUtil.GROUP_SEPARATOR);
		final int offset = childSepIdx + ClassNameUtil.GROUP_SEPARATOR.length();

		String parentName;
		String childName;
		if (childSepIdx > 0 && offset < name.length()) {
			parentName = name.substring(0, childSepIdx);
			childName = name.substring(offset);
		} else {
			parentName = ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE;
			childName = name;
		}

		NamePartsHolder result = new NamePartsHolder();
		result.parentName = parentName;
		result.childName = childName;

		return result;
	}

	/*
	 * Generates a full package name for the given parent package and the expression representing a child package. The
	 * expression is treated as a simple group like <i>some_group</i> or a complex group like
	 * <i>grand_parent_group::parent_group::some_group</i>.
	 */
	private static String generatePackageName(String parentPackage, String expression) {
		String childPackage = expression.replaceAll("\\s", ClassNameUtil.BLANK_CODE);
		Map<String, String> escapeSymbols = ClassNameUtil.getEscapeSymbols();
		for (String smb : escapeSymbols.keySet()) {
			childPackage = childPackage.replace(smb, escapeSymbols.get(smb));
		}
		childPackage = childPackage.replaceAll(GROUP_SEPARATOR, DOT_STR).toLowerCase();

		StringBuilder result = new StringBuilder();
		result.append(parentPackage).append(DOT).append(childPackage);

		return result.toString();
	}

	public interface NameParts {

		String getParentName();

		String getChildName();
	}

	private static class NamePartsHolder implements NameParts {

		String parentName;

		String childName;

		public String getParentName() {
			return parentName;
		}

		public String getChildName() {
			return childName;
		}

	}

}
