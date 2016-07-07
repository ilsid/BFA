package com.ilsid.bfa.script;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.IOHelper;

public class ScriptSourcePreprocessorUnitTest extends BaseUnitTestCase {

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

	private void assertExpressionsProcessing(String input, String expectedResult) throws Exception {
		ScriptingRepositoryInitializer.init();

		try {
			// Varargs processing is necessary before expressions processing
			final String sourceAfterVarargsProcessing = ScriptSourcePreprocessor
					.processVarargs(IOHelper.loadScript(input));

			String result = ScriptSourcePreprocessor.processExpressions(sourceAfterVarargsProcessing).getSource();

			assertEquals(IOHelper.loadScript(expectedResult), result);
		} finally {
			ScriptingRepositoryInitializer.cleanup();
		}
	}

}
