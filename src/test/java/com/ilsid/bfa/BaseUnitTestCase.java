package com.ilsid.bfa;

import java.lang.reflect.Field;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.internal.ExpectationBuilder;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class BaseUnitTestCase {

	private Mockery mockery = new JUnit4Mockery() {
		{
			setImposteriser(ClassImposteriser.INSTANCE);
		}
	};
	
	@Rule
	public ExpectedException exceptionRule = ExpectedException.none();
	
	protected void setInaccessibleField(Object target, String fieldName, Object fieldValue)
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, fieldValue);
	}

	protected void setInaccessibleParentField(Object target, String fieldName, Object fieldValue)
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {

		Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, fieldValue);
	}

	protected void assertEquals(Object expected, Object actual) {
		Assert.assertEquals(expected, actual);
	}

	protected void assertSame(Object expected, Object actual) {
		Assert.assertSame(expected, actual);
	}

	protected void assertNull(Object object) {
		Assert.assertNull(object);
	}

	protected void assertTrue(boolean condition) {
		Assert.assertTrue(condition);
	}

	protected void assertFalse(boolean condition) {
		Assert.assertFalse(condition);
	}

	protected void fail(String msg) {
		Assert.fail(msg);
	}

	protected <T> T mock(Class<T> clazz) {
		return mockery.mock(clazz);
	}

	protected void checking(ExpectationBuilder expectations) {
		mockery.checking(expectations);
	}
	
	protected void assertIsSatisfed() {
		mockery.assertIsSatisfied();
	}

}
