package com.ilsid.bfa.common;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class ClassNameUtilUnitTest extends BaseUnitTestCase {

	private static final String TEST_SCRIPT_NAME = "TestScript";

	private static final String EXPRESSION_CLASS_NAME_PREFIX = ClassNameUtil.GENERATED_PACKAGE + TEST_SCRIPT_NAME
			+ "$$";

	@Test
	public void generatedClassNameDoesNotContainSpaces() {
		assertEquals("AB", getScriptClassName("A B"));
		assertEquals("AB", getScriptClassName(" AB "));
		assertEquals("ABC", getScriptClassName(" A B C "));

		assertEquals("AB", getExpressionClassNamePart("A B"));
		assertEquals("AB", getExpressionClassNamePart(" AB "));
		assertEquals("ABC", getExpressionClassNamePart(" A B C "));
	}

	@Test
	public void generatedClassNameContainsReaplacedDots() {
		assertEquals("A%dtB", getScriptClassName("A.B"));
		assertEquals("%dtAB%dt", getScriptClassName(".AB."));
		assertEquals("%dtA%dtB%dtC%dt", getScriptClassName(".A.B.C."));

		assertEquals("A%dtB", getExpressionClassNamePart("A.B"));
		assertEquals("%dtAB%dt", getExpressionClassNamePart(".AB."));
		assertEquals("%dtA%dtB%dtC%dt", getExpressionClassNamePart(".A.B.C."));
	}

	@Test
	public void generatedClassNameContainsReplacedMinusSign() {
		assertEquals("A_Mns_B", getScriptClassName("A-B"));
		assertEquals("A_Mns_B", getScriptClassName("A - B"));
		
		assertEquals("A_Mns_B", getExpressionClassNamePart("A-B"));
		assertEquals("A_Mns_B", getExpressionClassNamePart("A - B"));
	}

	@Test
	public void generatedClassNameContainsReplacedPlusSign() {
		assertEquals("A_Pls_B", getScriptClassName("A+B"));
		assertEquals("A_Pls_B", getScriptClassName("A + B"));
		
		assertEquals("A_Pls_B", getExpressionClassNamePart("A+B"));
		assertEquals("A_Pls_B", getExpressionClassNamePart("A + B"));
	}

	@Test
	public void generatedClassNameContainsReplacedMultiplySign() {
		assertEquals("A_Mlt_B", getScriptClassName("A*B"));
		assertEquals("A_Mlt_B", getScriptClassName("A * B"));
		
		assertEquals("A_Mlt_B", getExpressionClassNamePart("A*B"));
		assertEquals("A_Mlt_B", getExpressionClassNamePart("A * B"));
	}

	@Test
	public void generatedClassNameContainsReplacedDivisionSign() {
		assertEquals("A_Div_B", getScriptClassName("A/B"));
		assertEquals("A_Div_B", getScriptClassName("A / B"));
		
		assertEquals("A_Div_B", getExpressionClassNamePart("A/B"));
		assertEquals("A_Div_B", getExpressionClassNamePart("A / B"));
	}

	private String getExpressionClassNamePart(String expression) {
		String className = ClassNameUtil.generateClassName(TEST_SCRIPT_NAME, expression);
		assertTrue(className.startsWith(EXPRESSION_CLASS_NAME_PREFIX));

		return className.substring(EXPRESSION_CLASS_NAME_PREFIX.length());
	}
	
	private String getScriptClassName(String scriptName) {
		String className = ClassNameUtil.generateClassName(scriptName);
		assertTrue(className.startsWith(ClassNameUtil.GENERATED_PACKAGE));
		
		return className.substring(ClassNameUtil.GENERATED_PACKAGE.length());
	}

}
