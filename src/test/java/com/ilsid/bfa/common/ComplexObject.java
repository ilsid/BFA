package com.ilsid.bfa.common;

import java.util.List;

public class ComplexObject {
	
	private String strField;
	
	private List<SimpleObject> someList;

	public String getStrField() {
		return strField;
	}

	public void setStrField(String strField) {
		this.strField = strField;
	}
	
	public List<SimpleObject> getSomeList() {
		return someList;
	}
	
	public void setSomeList(List<SimpleObject> someList) {
		this.someList = someList;
	}

}
