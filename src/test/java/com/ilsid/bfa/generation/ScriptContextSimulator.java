package com.ilsid.bfa.generation;

import com.ilsid.bfa.test.types.Subscriber;

public class ScriptContextSimulator {
	
	public Object getLocalVar(String name) {
		Subscriber sb = new Subscriber();
		sb.PrepaidAmount = 10.55;
		sb.PrepaidReserved = 7.87;
		
		return sb;
	}

}
