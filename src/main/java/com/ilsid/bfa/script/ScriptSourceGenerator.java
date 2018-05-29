package com.ilsid.bfa.script;

import java.io.IOException;
import java.util.List;

import com.ilsid.bfa.common.JsonUtil;
import com.ilsid.bfa.flow.FlowDefinition;
import com.ilsid.bfa.flow.FlowDefinition.Block;
import com.ilsid.bfa.flow.FlowDesign;

/**
 * Generates script source.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptSourceGenerator {

	private static final String BLANK_REGEX = "\\s+";

	private static final String NL = "\n";

	private static final String SC = ";";

	private static final String COMMA = ", ";

	private static final String DQ = "\"";

	private static final String DECLARE_INPUT_VAR_EXPR_PATTERN = "DeclareInputVar(\"%s\", \"%s\");";

	private static final String DECLARE_LOCAL_VAR_EXPR_PATTERN = "DeclareLocalVar(\"%s\", \"%s\");";

	private static final String ACTION_EXPR_PATTERN = "Action(\"%s\"%s)";

	private static final String ACTION_RESULT_EXPR_PATTERN = ".SetResult(\"%s\");";

	private static final String SET_LOCAL_VAR_EXPR_PATTERN = "SetLocalVar(\"%s\", \"%s\");";

	private static final String COMMENTS_PATTERN = "/* %s */";

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
		processLocalVariables(code, flow);

		for (Block block : flow.getBlocks()) {
			BlockSourceGenerator.processBlock(code, block);
		}

		return code.toString();
	}

	private static FlowDefinition deserialize(String source) throws ParsingException {
		FlowDefinition flowDefinition;
		try {
			FlowDesign flowDesign = JsonUtil.toObject(source, FlowDesign.class);
			flowDefinition = flowDesign.getFlow();
		} catch (IOException e) {
			throw new ParsingException(String.format("Failed to deserialize a flow definition: \n%s", source), e);
		}

		return flowDefinition;
	}

	private static void processInputParameters(StringBuilder code, FlowDefinition flow) throws ParsingException {
		final List<String> params = flow.getInputParameters();
		for (String param : params) {
			String[] tokens = param.split(BLANK_REGEX);

			if (tokens.length != 2) {
				throw new ParsingException(String.format(
						"Invalid expression for input parameter: [%s]. Expected [name type] expression.", param));
			}

			code.append(String.format(DECLARE_INPUT_VAR_EXPR_PATTERN, tokens[0], tokens[1])).append(NL);
		}

		if (params.size() > 0) {
			code.append(NL);
		}
	}

	private static void processLocalVariables(StringBuilder code, FlowDefinition flow) throws ParsingException {
		final List<String> vars = flow.getLocalVariables();
		for (String var : vars) {
			String[] tokens = var.split(BLANK_REGEX);

			if (tokens.length != 2) {
				throw new ParsingException(String
						.format("Invalid expression for local variable: [%s]. Expected [name type] expression.", var));
			}

			code.append(String.format(DECLARE_LOCAL_VAR_EXPR_PATTERN, tokens[0], tokens[1])).append(NL);
		}

		if (vars.size() > 0) {
			code.append(NL);
		}
	}

	private static abstract class BlockSourceGenerator {

		private static final String EMPTY = "";

		private static final String ASSIGN_REGEX = "\\s*=\\s*";

		private static final int INIT_SOURCE_LEVEL = 0;

		private static final String INDENT_SPACES = "    ";

		public static void processBlock(StringBuilder code, Block block) throws ParsingException {
			processBlock(code, block, INIT_SOURCE_LEVEL);
		}

		public static void processBlock(StringBuilder code, Block block, int level) throws ParsingException {
			BlockSourceGenerator sourceGenerator = BlockSourceGeneratorFactory.getGenerator(block.getType());
			if (sourceGenerator != null) {
				sourceGenerator.doProcessBlock(code, block, level);
			}
		}

		protected abstract void doProcessBlock(StringBuilder code, Block block, int level) throws ParsingException;

		protected void processAssignExpressions(List<String> expressions, StringBuilder code, String indent)
				throws ParsingException {
			for (String preExpr : expressions) {
				String[] tokens = preExpr.split(ASSIGN_REGEX);

				if (tokens.length != 2) {
					throw new ParsingException(String.format(
							"Invalid assignment expression: [%s]. Expected [var = value] expression.", preExpr));
				}

				code.append(String.format(SET_LOCAL_VAR_EXPR_PATTERN, tokens[0], tokens[1])).append(NL).append(indent);
			}
		}

		protected String getIndent(int level) {
			StringBuilder sb = new StringBuilder(EMPTY);
			for (int i = 0; i < level; i++) {
				sb.append(INDENT_SPACES);
			}

			return sb.toString();
		}

	}

	private static class ActionSourceGenerator extends BlockSourceGenerator {

		public void doProcessBlock(StringBuilder code, Block block, int level) throws ParsingException {
			final String indent = getIndent(level);
			code.append(indent);

			code.append(String.format(COMMENTS_PATTERN, block.getDescription())).append(NL).append(indent);

			List<String> exprs = block.getExpressions();

			if (exprs != null && exprs.size() > 0) {
				processAssignExpressions(exprs, code, indent);
			} else {
				processAssignExpressions(block.getPreExecExpressions(), code, indent);

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

				code.append(NL).append(indent);
				processAssignExpressions(block.getPostExecExpressions(), code, indent);
			}

			code.append(NL);
		}

	}

	// TODO: complete implementation
	private static class ConditionSourceGenerator extends BlockSourceGenerator {

		private static final String EQUAL_REGEX = "\\s*==\\s*";

		private static final String EQUAL_EXPR_PATTERN = "Equal(\"%s\", \"%s\")";

		private static final String IF_EXPR_PATTERN = "if (%s) {\n\n%s%s}";

		@Override
		protected void doProcessBlock(StringBuilder code, Block block, int level) throws ParsingException {
			final String indent = getIndent(level);
			code.append(indent);
			
			List<String> exprs = block.getExpressions();

			if (exprs.size() > 0) {
				String expr = exprs.get(0);
				String[] tokens = expr.split(EQUAL_REGEX);

				if (tokens.length != 2) {
					throw new ParsingException(String
							.format("Invalid equality expression: [%s]. Expected [var == value] expression.", expr));
				}

				code.append(String.format(COMMENTS_PATTERN, block.getDescription())).append(NL).append(indent);

				StringBuilder branchCode = new StringBuilder();
				for (Block branchBlock : block.getTrueBranch()) {
					BlockSourceGenerator.processBlock(branchCode, branchBlock, level + 1);
				}

				String equalExpr = String.format(EQUAL_EXPR_PATTERN, tokens[0], tokens[1]);
				String ifExpr = String.format(IF_EXPR_PATTERN, equalExpr, branchCode, indent);
				code.append(ifExpr).append(NL).append(NL);
			}

		}

	}

	// TODO: add other block source generator implementations
	private static class BlockSourceGeneratorFactory {

		private static final BlockSourceGenerator ACTION_SOURCE_GENERATOR = new ActionSourceGenerator();

		private static final BlockSourceGenerator CONDITION_SOURCE_GENERATOR = new ConditionSourceGenerator();

		public static BlockSourceGenerator getGenerator(String blockType) {

			if (FlowDefinition.BlockType.ACTION.equals(blockType)) {
				return ACTION_SOURCE_GENERATOR;
			} else if (FlowDefinition.BlockType.CONDITION.equals(blockType)) {
				return CONDITION_SOURCE_GENERATOR;
			}

			return null;
		}

	}

}
