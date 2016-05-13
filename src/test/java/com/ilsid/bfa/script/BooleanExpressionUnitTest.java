package com.ilsid.bfa.script;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.BooleanExpression;

public class BooleanExpressionUnitTest extends BaseUnitTestCase {
	
	@Test(expected = ScriptException.class)
	public void exceptionIsThrownOnNullInput() throws Exception {
		new BooleanExpression(null).getValue();
	}
	
	@Test(expected = ScriptException.class)
	public void exceptionIsThrownOnEmptyInput() throws Exception {
		new BooleanExpression(StringUtils.EMPTY).getValue();
	}
	
	@Test(expected = ScriptException.class)
	public void exceptionIsThrownOnIncorrectInput() throws Exception {
		new BooleanExpression("aaa").getValue();
	}
	
	@Test
	public void trueIsReturnedForLowerCaseTrueInput() throws Exception {
		assertEquals(Boolean.TRUE, new BooleanExpression("true").getValue());
	}
	
	@Test
	public void falseIsReturnedForLowerCaseFalseInput() throws Exception {
		assertEquals(Boolean.FALSE, new BooleanExpression("false").getValue());
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
