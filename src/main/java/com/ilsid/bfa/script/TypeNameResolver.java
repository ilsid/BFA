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
		int classSeparatorIndex = scriptName.lastIndexOf(ClassNameUtil.GROUP_SEPARATOR);

		String actualScriptName;
		String scriptGroupName;
		if (classSeparatorIndex > 0) {
			actualScriptName = scriptName.substring(classSeparatorIndex + ClassNameUtil.GROUP_SEPARATOR.length());
			scriptGroupName = scriptName.substring(0, classSeparatorIndex);
		} else {
			actualScriptName = scriptName;
			scriptGroupName = ClassNameUtil.DEFAULT_GROUP_SUBPACKAGE;
		}

		final String simpleClassName = ClassNameUtil.generateSimpleClassName(actualScriptName,
				ClassNameUtil.BLANK_CODE);

		return ClassNameUtil.generatePackageName(scriptGroupName) + DOT + simpleClassName.toLowerCase() + DOT
				+ simpleClassName;
	}

	public static String resolveExpressionClassName(String scriptName, String expression) {
		return resolveScriptClassName(scriptName) + EXPRESSION_PREFIX
				+ ClassNameUtil.generateSimpleClassName(expression, EMPTY);
	}

}
