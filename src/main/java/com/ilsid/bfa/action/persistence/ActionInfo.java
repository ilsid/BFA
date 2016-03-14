package com.ilsid.bfa.action.persistence;

import java.util.List;

/**
 * Action details.
 * 
 * @author illia.sydorovych
 *
 */
public class ActionInfo {

	private String implementationClassName;

	private List<String> dependencies;
	
	public ActionInfo(String implementationClassName, List<String> dependencies) {
		this.implementationClassName = implementationClassName;
		this.dependencies = dependencies;
	}

	public String getImplementationClassName() {
		return implementationClassName;
	}

	public List<String> getDependencies() {
		return dependencies;
	}
}
