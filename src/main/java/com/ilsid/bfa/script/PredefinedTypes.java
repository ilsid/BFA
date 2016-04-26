package com.ilsid.bfa.script;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.script.TypeValueResolver.PredefinedTypeResolver;
import com.ilsid.bfa.script.TypeValueResolver.IntegerResolver;
import com.ilsid.bfa.script.TypeValueResolver.DoubleResolver;
import com.ilsid.bfa.script.TypeValueResolver.StringResolver;

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
		
		resolvers = new HashMap<>();
		resolvers.put("java.lang.Integer", new IntegerResolver("Number"));
		resolvers.put("java.lang.Double", new DoubleResolver("Decimal"));
		resolvers.put("java.lang.String", new StringResolver("String"));
	}

	static String getJavaType(String typeName) {
		return types.get(typeName);
	}
	
	static TypeValueResolver getResolver(String className) {
		return resolvers.get(className);
	}

}
