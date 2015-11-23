package com.ilsid.bfa.script;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;

public class TypeNameResolverUnitTest extends BaseUnitTestCase {

	private static final String BASE_PACKAGE = "com.ilsid.bfa.generated.";

	private static final String SCRIPT_CLASS_NAME_PREFIX = BASE_PACKAGE + "script.default_group.";

	private static final String ACTION_CLASS_NAME_PREFIX = BASE_PACKAGE + "action.";

	private static final String ENTITY_CLASS_NAME_PREFIX = BASE_PACKAGE + "entity.default_group.";

	private static final String TEST_SCRIPT_NAME = "TestScript";

	private static final String EXPRESSION_CLASS_NAME_PREFIX = SCRIPT_CLASS_NAME_PREFIX + TEST_SCRIPT_NAME.toLowerCase()
			+ "." + TEST_SCRIPT_NAME + "$$";

	@Test
	public void generatedClassNameDoesNotContainSpaces() {
		assertEquals("A_x20_B", getScriptClassName("A B"));
		assertEquals("_x20_AB_x20_", getScriptClassName(" AB "));
		assertEquals("_x20_A_x20_B_x20_C_x20_", getScriptClassName(" A B C "));
		assertEquals(SCRIPT_CLASS_NAME_PREFIX + "a_x20_b.A_x20_B", TypeNameResolver.resolveScriptClassName("A B"));

		assertEquals("AB", getExpressionClassNamePart("A B"));
		assertEquals("AB", getExpressionClassNamePart(" AB "));
		assertEquals("ABC", getExpressionClassNamePart(" A B C "));

		assertEquals("A_x20_B", getActionClassName("A B"));
		assertEquals("_x20_AB_x20_", getActionClassName(" AB "));
		assertEquals("_x20_A_x20_B_x20_C_x20_", getActionClassName(" A B C "));
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
		assertEquals("A_x20__Mns__x20_B", getScriptClassName("A - B"));

		assertEquals("A_Mns_B", getExpressionClassNamePart("A-B"));
		assertEquals("A_Mns_B", getExpressionClassNamePart("A - B"));

		assertEquals("A_Mns_B", getActionClassName("A-B"));
		assertEquals("A_x20__Mns__x20_B", getActionClassName("A - B"));

	}

	@Test
	public void generatedClassNameContainsReplacedPlusSign() {
		assertEquals("A_Pls_B", getScriptClassName("A+B"));
		assertEquals("A_x20__Pls__x20_B", getScriptClassName("A + B"));

		assertEquals("A_Pls_B", getExpressionClassNamePart("A+B"));
		assertEquals("A_Pls_B", getExpressionClassNamePart("A + B"));

		assertEquals("A_Pls_B", getActionClassName("A+B"));
		assertEquals("A_x20__Pls__x20_B", getActionClassName("A + B"));
	}

	@Test
	public void generatedClassNameContainsReplacedMultiplySign() {
		assertEquals("A_Mlt_B", getScriptClassName("A*B"));
		assertEquals("A_x20__Mlt__x20_B", getScriptClassName("A * B"));

		assertEquals("A_Mlt_B", getExpressionClassNamePart("A*B"));
		assertEquals("A_Mlt_B", getExpressionClassNamePart("A * B"));

		assertEquals("A_Mlt_B", getActionClassName("A*B"));
		assertEquals("A_x20__Mlt__x20_B", getActionClassName("A * B"));
	}

	@Test
	public void generatedClassNameContainsReplacedDivisionSign() {
		assertEquals("A_Div_B", getScriptClassName("A/B"));
		assertEquals("A_x20__Div__x20_B", getScriptClassName("A / B"));

		assertEquals("A_Div_B", getExpressionClassNamePart("A/B"));
		assertEquals("A_Div_B", getExpressionClassNamePart("A / B"));

		assertEquals("A_Div_B", getActionClassName("A/B"));
		assertEquals("A_x20__Div__x20_B", getActionClassName("A / B"));
	}

	@Test
	public void predefinedNumberTypeCanBeResolved() {
		assertEquals("java.lang.Integer", TypeNameResolver.resolveEnityClassName("Number"));
	}

	@Test
	public void predefinedDecimalTypeCanBeResolved() {
		assertEquals("java.lang.Double", TypeNameResolver.resolveEnityClassName("Decimal"));
	}

	@Test
	public void nonPredefinedEntityTypeCanBeResolved() {
		assertEquals(ENTITY_CLASS_NAME_PREFIX + "SomeBean", TypeNameResolver.resolveEnityClassName("SomeBean"));
	}

	private String getExpressionClassNamePart(String expression) {
		String className = TypeNameResolver.resolveExpressionClassName(TEST_SCRIPT_NAME, expression);
		assertTrue(className.startsWith(EXPRESSION_CLASS_NAME_PREFIX));

		return className.substring(EXPRESSION_CLASS_NAME_PREFIX.length());
	}

	private String getScriptClassName(String scriptName) {
		String className = TypeNameResolver.resolveScriptClassName(scriptName);
		final String scriptPackage = SCRIPT_CLASS_NAME_PREFIX
				+ className.substring(className.lastIndexOf('.') + 1).toLowerCase() + ".";
		assertTrue(className.startsWith(scriptPackage));

		return className.substring(scriptPackage.length());
	}

	private String getActionClassName(String scriptName) {
		String className = TypeNameResolver.resolveActionClassName(scriptName);
		assertTrue(className.startsWith(ACTION_CLASS_NAME_PREFIX));

		return className.substring(ACTION_CLASS_NAME_PREFIX.length());
	}

}
