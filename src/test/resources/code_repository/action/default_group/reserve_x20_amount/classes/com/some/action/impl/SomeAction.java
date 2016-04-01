package com.some.action.impl;

//joda-time jar is needed in classpath
import org.joda.time.Months;

// mail jar is needed in classpath
import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionException;

public class SomeAction implements Action {

	@SuppressWarnings("unused")
	public Object[] execute() throws ActionException {
		Months month = Months.EIGHT;
		Address address = new InternetAddress();
		return new Object[] { new ActionResultProvider().getResult() };
	}

	public void setInputParameters(Object[] params) {
	}

}
