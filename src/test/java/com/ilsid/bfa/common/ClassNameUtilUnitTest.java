package com.ilsid.bfa.common;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class ClassNameUtilUnitTest extends BaseUnitTestCase {

	@Test
	public void classNameWithCompositePackageIsConvertedToDirsChain() {
		assertEquals(toNativeFS("com/foo/bar"), ClassNameUtil.getDirs("com.foo.bar.MyClass"));
	}

	@Test
	public void classNameWithSinglePackageIsConvertedToSingleDir() {
		assertEquals("com", ClassNameUtil.getDirs("com.MyClass"));
	}

	@Test
	public void classNameWithoutPackageIsFailedToBeConvertedToDirs() {
		// Precondition: class name must contain a package
		exceptionRule.expect(IndexOutOfBoundsException.class);
		ClassNameUtil.getDirs("MyClass");
	}

	@Test
	public void shortClassNameForSimplePackageCanBeResolved() {
		assertEquals("Foo", ClassNameUtil.getShortClassName("com.Foo"));
	}

	@Test
	public void shortClassNameForComplexPackageCanBeResolved() {
		assertEquals("Foo", ClassNameUtil.getShortClassName("com.my.Foo"));
	}

	@Test
	public void shortClassNameForNoPackageCanBeResolved() {
		assertEquals("Foo", ClassNameUtil.getShortClassName("Foo"));
	}

	@Test
	public void simplePackageNameForClassNameCanBeResolved() {
		assertEquals("com", ClassNameUtil.getPackageName("com.Foo"));
	}

	@Test
	public void complexPackageNameForClassNameCanBeResolved() {
		assertEquals("com.my", ClassNameUtil.getPackageName("com.my.Foo"));
	}

	@Test
	public void packageNameForClassNameWithoutPackageFailedToBeResolved() {
		// Precondition: class name must contain a package
		exceptionRule.expect(IndexOutOfBoundsException.class);
		ClassNameUtil.getPackageName("Foo");
	}

}
