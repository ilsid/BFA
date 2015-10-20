package com.ilsid.bfa.common;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class ClassNameUtilUnitTest extends BaseUnitTestCase {
	
	private static final String BASE_PACKAGE = "com.ilsid.bfa.generated.";

	private static final String SCRIPT_CLASS_NAME_PREFIX = BASE_PACKAGE + "script.";

	private static final String ACTION_CLASS_NAME_PREFIX = BASE_PACKAGE + "action.";

	private static final String TEST_SCRIPT_NAME = "TestScript";

	private static final String EXPRESSION_CLASS_NAME_PREFIX = SCRIPT_CLASS_NAME_PREFIX + TEST_SCRIPT_NAME + "$$";

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
		assertEquals("A%dtB", getScriptClassName("A.B"));
		assertEquals("%dtAB%dt", getScriptClassName(".AB."));
		assertEquals("%dtA%dtB%dtC%dt", getScriptClassName(".A.B.C."));

		assertEquals("A%dtB", getExpressionClassNamePart("A.B"));
		assertEquals("%dtAB%dt", getExpressionClassNamePart(".AB."));
		assertEquals("%dtA%dtB%dtC%dt", getExpressionClassNamePart(".A.B.C."));
		
		assertEquals("A%dtB", getActionClassName("A.B"));
		assertEquals("%dtAB%dt", getActionClassName(".AB."));
		assertEquals("%dtA%dtB%dtC%dt", getActionClassName(".A.B.C."));
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

	private String getExpressionClassNamePart(String expression) {
		String className = ClassNameUtil.resolveExpressionClassName(TEST_SCRIPT_NAME, expression);
		assertTrue(className.startsWith(EXPRESSION_CLASS_NAME_PREFIX));

		return className.substring(EXPRESSION_CLASS_NAME_PREFIX.length());
	}

	private String getScriptClassName(String scriptName) {
		String className = ClassNameUtil.resolveScriptClassName(scriptName);
		assertTrue(className.startsWith(SCRIPT_CLASS_NAME_PREFIX));

		return className.substring(SCRIPT_CLASS_NAME_PREFIX.length());
	}
	
	private String getActionClassName(String scriptName) {
		String className = ClassNameUtil.resolveActionClassName(scriptName);
		assertTrue(className.startsWith(ACTION_CLASS_NAME_PREFIX));

		return className.substring(ACTION_CLASS_NAME_PREFIX.length());
	}

}
