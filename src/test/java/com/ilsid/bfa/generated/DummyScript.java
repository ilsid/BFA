package com.ilsid.bfa.generated;

import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.script.Script;
import com.ilsid.bfa.script.ScriptException;

public class DummyScript extends Script {
	
	private String result;
	
	@Override
	protected void doExecute() throws ScriptException {
		result = TestConstants.DUMMY_SCRIPT_RESULT;
	}

	public String getResult() {
		return result;
	}
	
}
