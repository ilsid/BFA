package com.ilsid.bfa.script;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.TestConstants;
import com.ilsid.bfa.common.IOHelper;

public class ScriptSourceGeneratorUnitTest extends BaseUnitTestCase {

	private static final String FLOW_JSON_DIR = TestConstants.TEST_RESOURCES_DIR
			+ "/dynamicCode/flow-diagram-representation/";

	@Test
	public void flowInputParamsAndLocalVarsAreParsed() throws Exception {
		assertScriptCode("input-params-and-local-vars.json", "input-params-and-local-vars-expected-script.txt");
	}

	@Test
	public void actionBlocksAreParsed() throws Exception {
		assertScriptCode("block-actions.json", "block-actions-expected-script.txt");
	}
	
	@Test
	public void actionBlockWithExpressionIsParsed() throws Exception {
		assertScriptCode("block-action-expression-only.json", "block-action-expression-only-expected-script.txt");
	}
	
	@Test
	public void wholeFlowWithActionsIsParsed() throws Exception {
		assertScriptCode("flow-with-actions.json", "flow-with-actions-expected-script.txt");
	}
	
	private void assertScriptCode(String inFlowFileName, String outScriptFileName) throws Exception {
		String json = loadContents(inFlowFileName);
		String script = ScriptSourceGenerator.generate(json);
		assertExpectedOutput(script, outScriptFileName);
	}

	private void assertExpectedOutput(String script, String fileName) throws Exception {
		String expectedResult = loadContents(fileName);
		assertEquals(toUnixLF(expectedResult.trim()), toUnixLF(script.trim()));
	}

	private String loadContents(String fileName) throws Exception {
		return IOHelper.loadFileContents(FLOW_JSON_DIR, fileName);
	}

}
