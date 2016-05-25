package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.script.TypeValueResolver.PredefinedTypeResolver;
import com.ilsid.bfa.script.TypeValueResolver.IntegerResolver;
import com.ilsid.bfa.script.TypeValueResolver.DoubleResolver;
import com.ilsid.bfa.script.TypeValueResolver.StringResolver;
import com.ilsid.bfa.script.TypeValueResolver.BooleanResolver;

/**
 * Predefined scripting types.
 * 
 * @author illia.sydorovych
 *
 */
class PredefinedTypes {

	private static final Map<String, String> types;
	
	private static final Map<String, PredefinedTypeResolver> resolvers;

	static {
		types = new HashMap<>();

		types.put("Number", "java.lang.Integer");
		types.put("Decimal", "java.lang.Double");
		types.put("String", "java.lang.String");
		types.put("Boolean", "java.lang.Boolean");
		
		resolvers = new HashMap<>();
		resolvers.put("java.lang.Integer", new IntegerResolver());
		resolvers.put("java.lang.Double", new DoubleResolver());
		resolvers.put("java.lang.String", new StringResolver());
		resolvers.put("java.lang.Boolean", new BooleanResolver());
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
