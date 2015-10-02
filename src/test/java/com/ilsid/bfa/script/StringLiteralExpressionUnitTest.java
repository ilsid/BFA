package com.ilsid.bfa.script;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.StringLiteralExpression;

public class StringLiteralExpressionUnitTest extends BaseUnitTestCase {
	
	@Test
	public void nullValueIsReturnedOnNullInput() {
		assertNull(new StringLiteralExpression(null).getValue());
	}
	
	@Test
	public void emptyValueIsReturnedOnEmptyInput() {
		assertEquals(StringUtils.EMPTY, new StringLiteralExpression("").getValue());
	}
	
	@Test
	public void sameValueIsReturnedOnNonEmptyInput() {
		String value = "some_val";
		assertSame(value, new StringLiteralExpression(value).getValue());
	}

}
