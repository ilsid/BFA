package com.ilsid.bfa;

import java.io.File;
import java.lang.reflect.Field;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.internal.ExpectationBuilder;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class BaseUnitTestCase {
	
	protected final static String REPOSITORY_ROOT_DIR_PATH = TestConstants.TEST_RESOURCES_DIR + "/__tmp_class_repository";
	
	protected final static File REPOSITORY_ROOT_DIR = new File(REPOSITORY_ROOT_DIR_PATH);

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
	
	protected void assertNotSame(Object expected, Object actual) {
		Assert.assertNotSame(expected, actual);
	}

	protected void assertNull(Object object) {
		Assert.assertNull(object);
	}
	
	protected void assertNotNull(Object object) {
		Assert.assertNotNull(object);
	}

	protected void assertTrue(boolean condition) {
		Assert.assertTrue(condition);
	}
	
	protected void assertTrue(String message, boolean condition) {
		Assert.assertTrue(message, condition);
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
	
	protected String toNativeFS(String dirs) {
		return dirs.replace('/', File.separatorChar);
	}

}
