package com.ilsid.bfa.script;

import org.junit.Ignore;
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
	
	@Test
	@Ignore
	public void conditionBlockWithTrueBranchOnlyIsParsed() throws Exception {
		assertScriptCode("condition-true-branch-only.json", "condition-true-branch-only-expected-script.txt");
	}

	@Test
	public void invalidFlowInputParamIsNotParsed() throws Exception {
		assertException("invalid-flow-input-param.json",
				"Invalid expression for input parameter: [name  ]. Expected [name type] expression.");
	}

	@Test
	public void invalidFlowLocalVarIsNotParsed() throws Exception {
		assertException("invalid-flow-local-var.json",
				"Invalid expression for local variable: [OSS::Site]. Expected [name type] expression.");
	}
	
	@Test
	public void invalidAssignExpressionIsNotParsed() throws Exception {
		assertException("invalid-assign-expression.json",
				"Invalid assignment expression: [zSite = ]. Expected [var = value] expression.");
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

	private void assertException(String flowFileName, String expectedMsg) throws Exception {
		try {
			String json = loadContents(flowFileName);
			ScriptSourceGenerator.generate(json);
			fail(ParsingException.class.getName() + " is expected");
		} catch (ParsingException e) {
			assertEquals(expectedMsg, e.getMessage());
		}
	}

}
