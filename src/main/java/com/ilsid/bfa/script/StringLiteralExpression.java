package com.ilsid.bfa.script;

class StringLiteralExpression implements Expression<String> {
	
	private String input;
	
	public StringLiteralExpression(String input) {
		this.input = input;
	}

	@Override
	public String getValue() {
		return input;
	}

}
