package com.some.action.impl;

// commons-collections jar is needed in classpath
import org.apache.commons.collections.CollectionUtils;

import java.util.Collection;

// mail jar is needed in classpath
import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionException;

// Updated implementation comparing with the one from code_repository/action
public class SomeAction implements Action {

	@SuppressWarnings("unused")
	public Object[] execute() throws ActionException {
		Collection<?> collection = CollectionUtils.EMPTY_COLLECTION;
		Address address = new InternetAddress();
		// Added " Updated" string to the result
		return new Object[] { new ActionResultProvider().getResult().concat(" Updated") };
	}

	public void setInputParameters(Object[] params) {
	}

}
