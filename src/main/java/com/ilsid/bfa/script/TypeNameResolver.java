package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.common.ClassNameUtil;

//TODO: write javadocs
public class TypeNameResolver {

	private static final char DOT = '.';

	private static final String GENERATED_ROOT_PACKAGE = ClassNameUtil.GENERATED_CLASSES_PACKAGE + DOT;

	private static final String GENERATED_SCRIPT_ROOT_PACKAGE = GENERATED_ROOT_PACKAGE + "script.";

	private static final String GENERATED_SCRIPT_DEFAULT_GROUP_PACKAGE = GENERATED_SCRIPT_ROOT_PACKAGE
			+ "default_group.";

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
		final String simpleClassName = ClassNameUtil.generateSimpleClassName(scriptName, ClassNameUtil.BLANK_CODE);
		return generateScriptPackageName(simpleClassName) + simpleClassName;
	}

	public static String resolveExpressionClassName(String scriptName, String expression) {
		final String scriptSimpleClassName = ClassNameUtil.generateSimpleClassName(scriptName,
				ClassNameUtil.BLANK_CODE);
		return generateScriptPackageName(scriptSimpleClassName) + scriptSimpleClassName + EXPRESSION_PREFIX
				+ ClassNameUtil.generateSimpleClassName(expression, EMPTY);
	}

	private static String generateScriptPackageName(String simpleScriptClassName) {
		return GENERATED_SCRIPT_DEFAULT_GROUP_PACKAGE + simpleScriptClassName.toLowerCase() + DOT;
	}

}
