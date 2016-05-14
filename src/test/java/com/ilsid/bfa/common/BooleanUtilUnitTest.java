package com.ilsid.bfa.common;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class BooleanUtilUnitTest extends BaseUnitTestCase {
	
	@Test
	public void booleanValuesAreRecognized() {
		assertTrue(BooleanUtil.isBoolean("true"));
		assertTrue(BooleanUtil.isBoolean("True"));
		assertTrue(BooleanUtil.isBoolean("TRUE"));
		assertTrue(BooleanUtil.isBoolean("TrUe"));
		assertTrue(BooleanUtil.isBoolean("false"));
		assertTrue(BooleanUtil.isBoolean("False"));
		assertTrue(BooleanUtil.isBoolean("FALSE"));
		assertTrue(BooleanUtil.isBoolean("FaLSe"));
		
		assertFalse(BooleanUtil.isBoolean(""));
		assertFalse(BooleanUtil.isBoolean(null));
		assertFalse(BooleanUtil.isBoolean("abc"));
		assertFalse(BooleanUtil.isBoolean("123"));
		assertFalse(BooleanUtil.isBoolean("off"));
		assertFalse(BooleanUtil.isBoolean("no"));
	}

}
