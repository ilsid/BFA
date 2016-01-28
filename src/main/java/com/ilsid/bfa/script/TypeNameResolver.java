package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.common.ClassNameUtil;

//TODO: write javadocs
public class TypeNameResolver {

	private static final char DOT = '.';

	private static final String GENERATED_ROOT_PACKAGE = ClassNameUtil.GENERATED_CLASSES_PACKAGE + DOT;

	private static final String EMPTY = "";

	private static final String GENERATED_ENTITY_ROOT_PACKAGE = GENERATED_ROOT_PACKAGE + "entity.";

	private static final String GENERATED_ENTITY_DEFAULT_GROUP_PACKAGE = GENERATED_ENTITY_ROOT_PACKAGE
			+ "default_group.";

	private static final String EXPRESSION_PREFIX = "$$";

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
		return ClassNameUtil.generatePackageName(ClassNameUtil.GENERATED_SCRIPTS_ROOT_PACKAGE, groupName);
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
