package com.ilsid.bfa.script;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.BooleanExpression;
import com.ilsid.bfa.script.ScriptException;

public class BooleanExpressionUnitTest extends BaseUnitTestCase {
	
	@Test(expected = IllegalArgumentException.class)
	public void exceptionIsThrownOnNullInput() throws Exception {
		new BooleanExpression(null).getValue();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void exceptionIsThrownOnEmptyInput() throws Exception {
		new BooleanExpression(StringUtils.EMPTY).getValue();
	}
	
	@Test(expected = ScriptException.class)
	public void exceptionIsThrownOnIncorrectInput() throws Exception {
		new BooleanExpression("aaa").getValue();
	}
	
	@Test(expected = ScriptException.class)
	public void exceptionIsThrownOnLowerCaseTrueInput() throws Exception {
		new BooleanExpression("true").getValue();
	}
	
	@Test(expected = ScriptException.class)
	public void exceptionIsThrownOnLowerCaseFalseInput() throws Exception {
		new BooleanExpression("false").getValue();
	}
	
	@Test
	public void trueIsReturnedForCapitalizedtTrueInput() throws Exception {
		assertEquals(Boolean.TRUE, new BooleanExpression("True").getValue());
	}
	
	@Test
	public void falseIsReturnedForCapitalizedFalseInput() throws Exception {
		assertEquals(Boolean.FALSE, new BooleanExpression("False").getValue());
	}

}
