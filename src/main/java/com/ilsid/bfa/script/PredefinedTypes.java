package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.script.TypeValueResolver.PredefinedTypeResolver;
import com.ilsid.bfa.script.TypeValueResolver.IntegerResolver;
import com.ilsid.bfa.script.TypeValueResolver.DoubleResolver;
import com.ilsid.bfa.script.TypeValueResolver.StringResolver;
import com.ilsid.bfa.script.TypeValueResolver.BooleanResolver;
import com.ilsid.bfa.script.TypeValueResolver.ArrayResolver;

/**
 * Predefined scripting types.
 * 
 * @author illia.sydorovych
 *
 */
class PredefinedTypes {

	public static final String NUMBER = "Number";

	public static final String DECIMAL = "Decimal";

	public static final String STRING = "String";

	public static final String BOOLEAN = "Boolean";

	public static final String ARRAY = "Array";

	private static final Map<String, String> types;

	private static final Map<String, PredefinedTypeResolver> resolvers;

	static {
		types = new HashMap<>();

		// Predefined types are mapped to canonical names
		types.put(NUMBER, "java.lang.Integer");
		types.put(DECIMAL, "java.lang.Double");
		types.put(STRING, "java.lang.String");
		types.put(BOOLEAN, "java.lang.Boolean");
		types.put(ARRAY, "java.lang.Object[]");

		resolvers = new HashMap<>();
		resolvers.put("java.lang.Integer", new IntegerResolver());
		resolvers.put("java.lang.Double", new DoubleResolver());
		resolvers.put("java.lang.String", new StringResolver());
		resolvers.put("java.lang.Boolean", new BooleanResolver());
		resolvers.put("java.lang.Object[]", new ArrayResolver());
	}

	static String getJavaType(String typeName) {
		return types.get(typeName);
	}

	static TypeValueResolver getResolver(String className) {
		return resolvers.get(className);
	}

	static boolean isPredefinedJavaType(String className) {
		return types.containsValue(className);
	}

}
