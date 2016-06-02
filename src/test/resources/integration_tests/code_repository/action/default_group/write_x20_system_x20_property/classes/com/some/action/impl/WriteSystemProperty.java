package com.some.action.impl;

// commons-collections jar is needed in classpath
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;

// mail jar is needed in classpath
import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionException;

public class WriteSystemProperty extends Action {

	@SuppressWarnings("unused")
	public Object[] execute() throws ActionException {
		Collection<?> collection = CollectionUtils.EMPTY_COLLECTION;
		Address address = new InternetAddress();

		String value = "Test Action Value";
		Object[] params = getInputParameters();
		
		if (params.length > 0) {
			value = value + " " + params[0];
		}
		if (params.length > 1) {
			value = value + " " + params[1];
		}

		System.setProperty("test.action.sys.property", value);

		return new Object[] { new ResultProvider().getResult() };
	}

}
