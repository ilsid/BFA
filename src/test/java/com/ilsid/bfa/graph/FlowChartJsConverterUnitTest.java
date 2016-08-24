package com.ilsid.bfa.graph;

import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.IOHelper;

public class FlowChartJsConverterUnitTest extends BaseUnitTestCase {

	@Test
	public void graphWithSimpleConditionCanBeConverted() throws Exception {
		assertExpectedOutput("input-vars-script.txt",
				"flowchartjs-adapter-expected-output/input-vars-script-representation.txt");
	}

	@Test
	public void graphWithSubConditionCanBeConverted() throws Exception {
		assertExpectedOutput("several-actions-and-subflows-with-params-script.txt",
				"flowchartjs-adapter-expected-output/several-actions-and-subflows-with-params-script-representation.txt");
	}

	private void assertExpectedOutput(String input, String expectedOutput) throws Exception {
		String source = IOHelper.loadScript(input);
		String actualResult = FlowChartJsConverter.buildFlowChart(source);
		String expectedResult = toUnixLF(IOHelper.loadScript(expectedOutput));

		assertEquals(expectedResult, actualResult);
	}

}
