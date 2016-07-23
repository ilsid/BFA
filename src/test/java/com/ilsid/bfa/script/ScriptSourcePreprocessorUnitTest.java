package com.ilsid.bfa.script;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.script.ScriptSourcePreprocessor.ExpressionsUnit;

public class ScriptSourcePreprocessorUnitTest extends BaseUnitTestCase {

	@BeforeClass
	public static void beforeClass() throws Exception {
		ScriptingRepositoryInitializer.init();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		ScriptingRepositoryInitializer.cleanup();
	}

	@Test
	public void varargsAreReplacedWithArrayExpressions() throws Exception {
		String result = ScriptSourcePreprocessor
				.processVarargs(IOHelper.loadScript("several-actions-and-subflows-with-params-script.txt"));

		assertEquals(
				IOHelper.loadScript(
						"preprocessor-expected-output/several-actions-and-subflows-with-params-script-after-varargs-processing.txt"),
				result);
	}

	@Test
	public void noChangesIfNoVarargs() throws Exception {
		final String origScript = IOHelper.loadScript("duplicated-expression-script.txt");

		String result = ScriptSourcePreprocessor.processVarargs(origScript);
		assertEquals(origScript, result);
	}

	@Test
	public void expressionsAreReplacedWithJavaCode() throws Exception {
		assertExpressionsProcessing("several-actions-and-subflows-with-params-script.txt",
				"preprocessor-expected-output/several-actions-and-subflows-with-params-script-after-expressions-processing.txt");
	}

	@Test
	public void expressionsWithEntitiesAreReplacedWithJavaCode() throws Exception {
		assertExpressionsProcessing("several-actions-with-params-and-entity-script.txt",
				"preprocessor-expected-output/several-actions-with-params-and-entity-script-after-expressions-processing.txt");
	}
	
	@Test
	public void expressionsWithArraysAreReplacedWithJavaCode() throws Exception {
		assertExpressionsProcessing("arrays-script.txt",
				"preprocessor-expected-output/arrays-script-after-expressions-processing.txt");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsInvalidExpression() throws Exception {
		assertProcessingErrors("one-invalid-expression-script.txt",
				"Could not parse expression [Var1 - Var33]: Number value or variable is expected after operand [-], but was [Var33]");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsMultipleInvalidExpressions() throws Exception {
		assertProcessingErrors("three-invalid-expressions-script.txt",
				"Could not parse expression [Var55]: Unexpected token [Var55]",
				"Could not parse expression [Var1 - Var33]: Number value or variable is expected after operand [-], but was [Var33]",
				"Could not parse expression [abc]: Unexpected token [abc]");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsInvalidEntityType() throws Exception {
		assertProcessingErrors("single-invalid-entity-script.txt", "Variable [Var1] has invalid type [Contract555]",
				"Could not parse expression [Var1.Days]: Unexpected token [Var1.Days]",
				"Could not parse expression [Var1.Days - Var2]: Unexpected token [Var1.Days]");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsLocalVarsThatCanNotBeResolved() throws Exception {
		assertProcessingErrors("local-vars-with-unresolved-values-script.txt", "[1.2] is not a value of type Number",
				"[abc] is not a value of type Decimal", "Could not parse expression [abc]: Unexpected token [abc]");
	}

	@Test
	public void errorDetailsAreProvidedIfScriptContainsDuplicatedInputAndLocalVars() throws Exception {
		assertProcessingErrors("duplicated-input-and-local-vars-script.txt",
				"Input variable with name [Var1] has been already declared");
	}

	private void assertExpressionsProcessing(String input, String expectedResult) throws Exception {
		// Varargs processing is necessary before expressions processing
		final String sourceAfterVarargsProcessing = ScriptSourcePreprocessor.processVarargs(IOHelper.loadScript(input));

		final ExpressionsUnit exprUnit = ScriptSourcePreprocessor.processExpressions(sourceAfterVarargsProcessing);
		String result = exprUnit.getSource();

		assertTrue(exprUnit.getProcessingErrors().isEmpty());
		assertEquals(toUnixLF(IOHelper.loadScript(expectedResult)), toUnixLF(result));
	}

	private void assertProcessingErrors(String input, String... expectedErrorMessages) throws Exception {
		// Varargs processing is necessary before expressions processing
		final String sourceAfterVarargsProcessing = ScriptSourcePreprocessor.processVarargs(IOHelper.loadScript(input));

		final ExpressionsUnit exprUnit = ScriptSourcePreprocessor.processExpressions(sourceAfterVarargsProcessing);
		assertNull(exprUnit.getSource());

		final List<Exception> processingErrors = exprUnit.getProcessingErrors();
		assertFalse(processingErrors.isEmpty());
		String[] actualErrorMessages = new String[processingErrors.size()];

		int idx = 0;
		for (Exception e : processingErrors) {
			actualErrorMessages[idx++] = e.getMessage();
		}

		assertTrue(Arrays.equals(expectedErrorMessages, actualErrorMessages));
	}

}
