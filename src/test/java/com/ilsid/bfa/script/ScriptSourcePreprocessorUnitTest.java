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
				IOHelper.loadScript("preprocessor-expected-output/several-actions-and-subflows-with-params-script.txt"),
				result);
	}

	@Test
	public void noChangesIfNoVarargs() throws Exception {
		final String origScript = IOHelper.loadScript("duplicated-expression-script.txt");

		String result = ScriptSourcePreprocessor.processVarargs(origScript);
		assertEquals(origScript, result);
	}

}
