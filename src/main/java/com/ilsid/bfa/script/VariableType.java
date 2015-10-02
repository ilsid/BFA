package com.ilsid.bfa.script;

enum VariableType {

	STRING("String"), BOOLEAN("Boolean"), DOUBLE("Double"), INTEGER("Integer"), OBJECT("Object");
	
	private String javaType;
	
	VariableType(String javaType) {
		this.javaType = javaType;
	}
	
	public String getJavaType() throws ScriptException {
		if (this != OBJECT) {
			return javaType;
		}
		
		String result = resolveObjectType();
		return result;
	}

	private String resolveObjectType() throws ScriptException {
		//TODO: call some kind of types dictionary
		return null;
	}

	public static VariableType resolve(String type) {
		switch (type) {
		case "Number":
			return INTEGER;
		case "Decimal":
			return DOUBLE;
		case "Boolean":
			return BOOLEAN;
		case "String":
			return STRING;
		default:
			return OBJECT;
		}
	}

}
