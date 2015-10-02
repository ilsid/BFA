package com.ilsid.bfa.script;

public class Variable {
	
	private String name;
	
	private String javaType;
	
	private Object value;
	
	public Variable(String name, String javaType) {
		this.name = name;
		this.javaType = javaType;
	}
	
	public Variable(String name, String javaType, Object value) {
		this.name = name;
		this.javaType = javaType;
		this.value = value;
	}

	public String getName() {
		return name;
	}
	
	public String getJavaType() {
		return javaType;
	}

	public Object getValue() {
		return value;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
}
