package com.ilsid.bfa.script;

public class ScriptContextUtil {
	
	public static ScriptContext createContext(final Variable... vars) {
		return new ScriptContext() {
			@Override
			public Variable getVar(String name) {
				for (Variable var : vars) {
					if (name.equals(var.getName())) {
						return var;
					}
				}
				return null;
			}
		};
	}

}
