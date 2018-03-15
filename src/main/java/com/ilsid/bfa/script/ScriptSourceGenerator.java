package com.ilsid.bfa.script;

import java.io.IOException;
import java.util.List;

import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.flow.FlowDefinition;
import com.ilsid.bfa.flow.FlowDefinition.Block;

/**
 * Generates script source.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptSourceGenerator {

	private static final String BLANK_REGEX = "\\s+";

	private static final String EQUAL_REGEX = "\\s*=\\s*";

	private static final String NL = "\n";

	private static final String SC = ";";

	private static final String COMMA = ", ";

	private static final String DQ = "\"";

	private static final String DECLARE_INPUT_VAR_EXPR_PATTERN = "DeclareInputVar(\"%s\", \"%s\");";

	private static final String DECLARE_LOCAL_VAR_EXPR_PATTERN = "DeclareLocalVar(\"%s\", \"%s\");";

	private static final String ACTION_EXPR_PATTERN = "Action(\"%s\"%s)";

	private static final String ACTION_RESULT_EXPR_PATTERN = ".SetResult(\"%s\");";

	private static final String SET_LOCAL_VAR_EXPR_PATTERN = "SetLocalVar(\"%s\", \"%s\");";

	/**
	 * Generates script source that is ready for the pre-processing stage.
	 * 
	 * @param flowDefinition
	 *            JSON representation of a scripting flow
	 * @return a script code ready for the pre-processing stage
	 * @throws ParsingException
	 * @see {@link ScriptSourcePreprocessor#processExpressions(String)}
	 */
	public static String generate(String flowDefinition) throws ParsingException {
		FlowDefinition flow = deserialize(flowDefinition);
		StringBuilder code = new StringBuilder();

		processInputParameters(code, flow);
		code.append(NL);
		processLocalVariables(code, flow);
		code.append(NL);

		for (Block block : flow.getBlocks()) {
			processBlock(code, block);
		}

		return code.toString();
	}

	private static FlowDefinition deserialize(String source) throws ParsingException {
		FlowDefinition res;
		try {
			res = JsonUtil.toObject(source, FlowDefinition.class);
		} catch (IOException e) {
			throw new ParsingException(String.format("Failed to deserialize a flow definition: \n%s", source), e);
		}

		return res;
	}

	private static void processInputParameters(StringBuilder source, FlowDefinition flow) throws ParsingException {
		for (String inParam : flow.getInputParameters()) {
			String[] tokens = inParam.split(BLANK_REGEX);

			if (tokens.length != 2) {
				throw new ParsingException(String.format(
						"Invalid expression for input parameter: [%s]. Expected [name type] expression.", inParam));
			}

			source.append(String.format(DECLARE_INPUT_VAR_EXPR_PATTERN, tokens[0], tokens[1])).append(NL);
		}

	}

	private static void processLocalVariables(StringBuilder source, FlowDefinition flow) throws ParsingException {
		for (String var : flow.getLocalVariables()) {
			String[] tokens = var.split(BLANK_REGEX);

			if (tokens.length != 2) {
				throw new ParsingException(String
						.format("Invalid expression for local variable: [%s]. Expected [name type] expression.", var));
			}

			source.append(String.format(DECLARE_LOCAL_VAR_EXPR_PATTERN, tokens[0], tokens[1])).append(NL);
		}
	}

	private static void processBlock(StringBuilder code, Block block) throws ParsingException {
		List<String> exprs = block.getExpressions();

		if (exprs != null && exprs.size() > 0) {
			processAssignExpressions(exprs, code);
			code.append(NL);
		} else {
			processAssignExpressions(block.getPreExecExpressions(), code);
			code.append(NL);

			String actionName = block.getName();
			StringBuilder paramsExpr = new StringBuilder();

			for (String param : block.getInputParameters()) {
				paramsExpr.append(COMMA).append(DQ).append(param).append(DQ);
			}
			code.append(String.format(ACTION_EXPR_PATTERN, actionName, paramsExpr.toString()));

			String output = block.getOutput();
			if (output != null && output.trim().length() > 0) {
				code.append(String.format(ACTION_RESULT_EXPR_PATTERN, output));
			} else {
				code.append(SC);
			}

			code.append(NL).append(NL);
			processAssignExpressions(block.getPostExecExpressions(), code);
		}
	}

	private static void processAssignExpressions(List<String> expressions, StringBuilder code) throws ParsingException {
		for (String preExpr : expressions) {
			String[] tokens = preExpr.split(EQUAL_REGEX);

			if (tokens.length != 2) {
				throw new ParsingException(String
						.format("Invalid assignment expression: [%s]. Expected [var = value] expression.", preExpr));
			}

			code.append(String.format(SET_LOCAL_VAR_EXPR_PATTERN, tokens[0], tokens[1])).append(NL);
		}
	}

}
