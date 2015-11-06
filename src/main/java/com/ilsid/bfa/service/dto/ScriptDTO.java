package com.ilsid.bfa.service.dto;

public class ScriptDTO {
	
	private static final String DEFAULT_ENCODING = "UTF-8";

	private String name;
	
	private String body;
	
	private String encoding = DEFAULT_ENCODING;
	
	public ScriptDTO() {
	}	
	
	public ScriptDTO(String name, String body) {
		this.name = name;
		this.body = body;
	}
	
	public ScriptDTO(String name, String body, String encoding) {
		this.name = name;
		this.body = body;
		this.encoding = encoding;
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

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String scriptEncoding) {
		this.encoding = scriptEncoding;
	}

}
