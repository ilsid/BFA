package com.ilsid.bfa.service.dto;

public class ScriptAdminParams extends TypeAdminParams {
	
	private String flowDiagram;
	
	public ScriptAdminParams() {
		super();
	}

	public ScriptAdminParams(String name, String body) {
		super(name, body);
	}

	public ScriptAdminParams(String name, String body, String title) {
		super(name, body, title);
	}

	public String getFlowDiagram() {
		return flowDiagram;
	}
	
	public void setFlowDiagram(String flowDiagram) {
		this.flowDiagram = flowDiagram;
	}
	
}

