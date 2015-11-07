package com.ilsid.bfa.script;

import java.io.File;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.script.ClassNameUtil;

public class ClassNameUtilUnitTest extends BaseUnitTestCase {

	private static final String BASE_PACKAGE = "com.ilsid.bfa.generated.";

	private static final String SCRIPT_CLASS_NAME_PREFIX = BASE_PACKAGE + "script.";

	private static final String ACTION_CLASS_NAME_PREFIX = BASE_PACKAGE + "action.";

	private static final String POJO_CLASS_NAME_PREFIX = BASE_PACKAGE + "type.";

	private static final String TEST_SCRIPT_NAME = "TestScript";

	private static final String EXPRESSION_CLASS_NAME_PREFIX = SCRIPT_CLASS_NAME_PREFIX + TEST_SCRIPT_NAME.toLowerCase()
			+ "." + TEST_SCRIPT_NAME + "$$";

	@Test
	public void generatedClassNameDoesNotContainSpaces() {
		assertEquals("AB", getScriptClassName("A B"));
		assertEquals("AB", getScriptClassName(" AB "));
		assertEquals("ABC", getScriptClassName(" A B C "));

		assertEquals("AB", getExpressionClassNamePart("A B"));
		assertEquals("AB", getExpressionClassNamePart(" AB "));
		assertEquals("ABC", getExpressionClassNamePart(" A B C "));

		assertEquals("AB", getActionClassName("A B"));
		assertEquals("AB", getActionClassName(" AB "));
		assertEquals("ABC", getActionClassName(" A B C "));
	}

	@Test
	public void generatedClassNameContainsReaplacedDots() {
		assertEquals("A_dt_B", getScriptClassName("A.B"));
		assertEquals("_dt_AB_dt_", getScriptClassName(".AB."));
		assertEquals("_dt_A_dt_B_dt_C_dt_", getScriptClassName(".A.B.C."));

		assertEquals("A_dt_B", getExpressionClassNamePart("A.B"));
		assertEquals("_dt_AB_dt_", getExpressionClassNamePart(".AB."));
		assertEquals("_dt_A_dt_B_dt_C_dt_", getExpressionClassNamePart(".A.B.C."));

		assertEquals("A_dt_B", getActionClassName("A.B"));
		assertEquals("_dt_AB_dt_", getActionClassName(".AB."));
		assertEquals("_dt_A_dt_B_dt_C_dt_", getActionClassName(".A.B.C."));
	}

	@Test
	public void generatedClassNameContainsReplacedMinusSign() {
		assertEquals("A_Mns_B", getScriptClassName("A-B"));
		assertEquals("A_Mns_B", getScriptClassName("A - B"));

		assertEquals("A_Mns_B", getExpressionClassNamePart("A-B"));
		assertEquals("A_Mns_B", getExpressionClassNamePart("A - B"));

		assertEquals("A_Mns_B", getActionClassName("A-B"));
		assertEquals("A_Mns_B", getActionClassName("A - B"));

	}

	@Test
	public void generatedClassNameContainsReplacedPlusSign() {
		assertEquals("A_Pls_B", getScriptClassName("A+B"));
		assertEquals("A_Pls_B", getScriptClassName("A + B"));

		assertEquals("A_Pls_B", getExpressionClassNamePart("A+B"));
		assertEquals("A_Pls_B", getExpressionClassNamePart("A + B"));

		assertEquals("A_Pls_B", getActionClassName("A+B"));
		assertEquals("A_Pls_B", getActionClassName("A + B"));
	}

	@Test
	public void generatedClassNameContainsReplacedMultiplySign() {
		assertEquals("A_Mlt_B", getScriptClassName("A*B"));
		assertEquals("A_Mlt_B", getScriptClassName("A * B"));

		assertEquals("A_Mlt_B", getExpressionClassNamePart("A*B"));
		assertEquals("A_Mlt_B", getExpressionClassNamePart("A * B"));

		assertEquals("A_Mlt_B", getActionClassName("A*B"));
		assertEquals("A_Mlt_B", getActionClassName("A * B"));
	}

	@Test
	public void generatedClassNameContainsReplacedDivisionSign() {
		assertEquals("A_Div_B", getScriptClassName("A/B"));
		assertEquals("A_Div_B", getScriptClassName("A / B"));

		assertEquals("A_Div_B", getExpressionClassNamePart("A/B"));
		assertEquals("A_Div_B", getExpressionClassNamePart("A / B"));

		assertEquals("A_Div_B", getActionClassName("A/B"));
		assertEquals("A_Div_B", getActionClassName("A / B"));
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
	public void classNameWithCompositePackageIsConvertedToDirsChain() {
		assertEquals(toNativeFS("com/foo/bar"), ClassNameUtil.getDirs("com.foo.bar.MyClass"));
	}

	@Test
	public void classNameWithSinglePackageIsConvertedToSingleDir() {
		assertEquals("com", ClassNameUtil.getDirs("com.MyClass"));
	}

	@Test
	public void classNameWithNoPackageIsFailedToConvertedToDirs() {
		// Precondition: class name must contain a package
		exceptionRule.expect(IndexOutOfBoundsException.class);
		ClassNameUtil.getDirs("MyClass");
	}

	@Test
	public void predefinedNumberTypeCanBeResolved() {
		assertEquals("Integer", ClassNameUtil.resolveJavaClassName("Number"));
	}

	@Test
	public void predefinedDecimalTypeCanBeResolved() {
		assertEquals("Double", ClassNameUtil.resolveJavaClassName("Decimal"));
	}

	@Test
	public void nonPredefinedPojoTypeCanBeResolved() {
		assertEquals(POJO_CLASS_NAME_PREFIX + "SomeBean", ClassNameUtil.resolveJavaClassName("SomeBean"));
	}

	private String getExpressionClassNamePart(String expression) {
		String className = ClassNameUtil.resolveExpressionClassName(TEST_SCRIPT_NAME, expression);
		assertTrue(className.startsWith(EXPRESSION_CLASS_NAME_PREFIX));

		return className.substring(EXPRESSION_CLASS_NAME_PREFIX.length());
	}

	private String getScriptClassName(String scriptName) {
		String className = ClassNameUtil.resolveScriptClassName(scriptName);
		final String scriptPackage = SCRIPT_CLASS_NAME_PREFIX
				+ className.substring(className.lastIndexOf('.') + 1).toLowerCase() + ".";
		assertTrue(className.startsWith(scriptPackage));

		return className.substring(scriptPackage.length());
	}

	private String getActionClassName(String scriptName) {
		String className = ClassNameUtil.resolveActionClassName(scriptName);
		assertTrue(className.startsWith(ACTION_CLASS_NAME_PREFIX));

		return className.substring(ACTION_CLASS_NAME_PREFIX.length());
	}

	private String toNativeFS(String dirs) {
		return dirs.replace('/', File.separatorChar);
	}

}