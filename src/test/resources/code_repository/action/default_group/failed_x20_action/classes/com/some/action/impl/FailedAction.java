package com.some.action.impl;

import com.ilsid.bfa.action.Action;
import com.ilsid.bfa.action.ActionException;

public class FailedAction extends Action {

	@Override
	public Object[] execute() throws ActionException {
		Exception rootException = new Exception("Base action error");
		throw new ActionException("Test action failed", rootException);
	}

}
