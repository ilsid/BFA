package com.ilsid.bfa.common;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.NumberUtil;

public class NumberUtilUnitTest extends BaseUnitTestCase {

	@Test
	public void doubleValuesAreRecognized() {
		assertTrue(NumberUtil.isDouble(".1"));
		assertTrue(NumberUtil.isDouble(".001"));
		assertTrue(NumberUtil.isDouble("0.23"));
		assertTrue(NumberUtil.isDouble("2.5"));
		assertTrue(NumberUtil.isDouble("25.37"));
		assertTrue(NumberUtil.isDouble("253.7"));
		assertTrue(NumberUtil.isDouble(".0"));
		assertTrue(NumberUtil.isDouble("0.0"));
		assertTrue(NumberUtil.isDouble("00.00"));
		
		assertTrue(NumberUtil.isDouble("-.1"));
		assertTrue(NumberUtil.isDouble("-.001"));
		assertTrue(NumberUtil.isDouble("-0.23"));
		assertTrue(NumberUtil.isDouble("-2.5"));
		assertTrue(NumberUtil.isDouble("-25.37"));
		assertTrue(NumberUtil.isDouble("-253.7"));
		assertTrue(NumberUtil.isDouble("-.0"));
		assertTrue(NumberUtil.isDouble("-0.0"));
		assertTrue(NumberUtil.isDouble("-00.00"));
		
		assertFalse(NumberUtil.isDouble("1"));
		assertFalse(NumberUtil.isDouble("123"));
		assertFalse(NumberUtil.isDouble("0"));
		assertFalse(NumberUtil.isDouble("-"));
		assertFalse(NumberUtil.isDouble("."));
		assertFalse(NumberUtil.isDouble("-"));
		assertFalse(NumberUtil.isDouble(""));
		assertFalse(NumberUtil.isDouble(null));
		assertFalse(NumberUtil.isDouble("abc"));
		assertFalse(NumberUtil.isDouble("12."));
		assertFalse(NumberUtil.isDouble("1.2.3"));
		assertFalse(NumberUtil.isDouble("12a3"));
		assertFalse(NumberUtil.isDouble("1.2a3"));
		assertFalse(NumberUtil.isDouble("12.5a3"));
		
		assertFalse(NumberUtil.isDouble("-1"));
		assertFalse(NumberUtil.isDouble("-123"));
		assertFalse(NumberUtil.isDouble("-0"));
		assertFalse(NumberUtil.isDouble("-abc"));
		assertFalse(NumberUtil.isDouble("-12."));
		assertFalse(NumberUtil.isDouble("-1.2.3"));
		assertFalse(NumberUtil.isDouble("-12a3"));
		assertFalse(NumberUtil.isDouble("-1.2a3"));
		assertFalse(NumberUtil.isDouble("-12.5a3"));
		
		assertFalse(NumberUtil.isDouble("a-bc"));
		assertFalse(NumberUtil.isDouble("1-2."));
		assertFalse(NumberUtil.isDouble("1-.2.3"));
		assertFalse(NumberUtil.isDouble("12.3-"));
		assertFalse(NumberUtil.isDouble("1-2a3"));
		assertFalse(NumberUtil.isDouble("1-.2a3"));
		assertFalse(NumberUtil.isDouble("1-2.5a3"));
	}
	
	@Test
	public void integerValuesAreRecognized() {
		assertTrue(NumberUtil.isInteger("1"));
		assertTrue(NumberUtil.isInteger("123"));
		assertTrue(NumberUtil.isInteger("0"));
		
		assertTrue(NumberUtil.isInteger("-1"));
		assertTrue(NumberUtil.isInteger("-123"));
		assertTrue(NumberUtil.isInteger("-0"));
		
		assertFalse(NumberUtil.isInteger(""));
		assertFalse(NumberUtil.isInteger(null));
		assertFalse(NumberUtil.isInteger("5.1"));
		assertFalse(NumberUtil.isInteger(".51"));
		assertFalse(NumberUtil.isInteger("2.51"));
		assertFalse(NumberUtil.isInteger("abc"));
		assertFalse(NumberUtil.isInteger("1a2"));
		assertFalse(NumberUtil.isInteger("a12"));
		assertFalse(NumberUtil.isInteger("12a"));
		assertFalse(NumberUtil.isInteger("1-2"));
		assertFalse(NumberUtil.isInteger("12-"));
	}

}
