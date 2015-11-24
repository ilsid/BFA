package com.ilsid.bfa.service.dto;

public abstract class TypeAdminParams {
	
	private static final String DEFAULT_ENCODING = "UTF-8";

	private String name;
	
	private String body;
	
	private String group;
	
	private String encoding = DEFAULT_ENCODING;
	
	public TypeAdminParams() {
	}	
	
	public TypeAdminParams(String name, String body) {
		this.name = name;
		this.body = body;
	}
	
	public TypeAdminParams(String name, String body, String group) {
		this.name = name;
		this.body = body;
		this.group = group;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String scriptName) {
		this.name = scriptName;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String scriptBody) {
		this.body = scriptBody;
	}

	public String getGroup() {
		return group;
	}
	
	public void setGroup(String group) {
		this.group = group;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String scriptEncoding) {
		this.encoding = scriptEncoding;
	}

}

