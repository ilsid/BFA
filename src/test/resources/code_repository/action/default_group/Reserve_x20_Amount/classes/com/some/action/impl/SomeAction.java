package com.some.action.impl;

// commons-collections jar is needed in classpath
import org.apache.commons.collections.CollectionUtils;
// mail jar is needed in classpath
import javax.mail.Address;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionException;

@SuppressWarnings("unused")
public class SomeAction implements Action {

	public Object[] execute() throws ActionException {
		return new Object[] { new ActionResultProvider().getResult() };
	}

	public void setInputParameters(Object[] params) {
	}

}
