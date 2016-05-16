package com.ilsid.bfa.script;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import com.ilsid.bfa.common.BooleanUtil;
import com.ilsid.bfa.common.NumberUtil;
import com.ilsid.bfa.persistence.DynamicClassLoader;

/**
 * Parses a scripting expression and returns a corresponding Java source code.
 * 
 * @author illia.sydorovych
 *
 */
public class ScriptExpressionParser {

	private ScriptContext scriptContext;

	/**
	 * Creates an instance.
	 * 
	 * @param scriptContext
	 *            script context
	 */
	public ScriptExpressionParser(ScriptContext scriptContext) {
		this.scriptContext = scriptContext;
	}

	/**
	 * Parses a scripting expression.
	 * 
	 * @param scriptExpression
	 *            scripting expression
	 * @return Java source code
	 * @throws ParsingException
	 *             in case of the parsing failure
	 */
	public String parse(String scriptExpression) throws ParsingException {
		ParsingMachine parsingMachine = new ParsingMachine(scriptExpression, scriptContext);
		try {
			parsingMachine.process();
		} catch (ParsingStateException e) {
			throw new ParsingException("Could not parse expression [" + scriptExpression + "]: " + e.getMessage(), e);
		}

		return parsingMachine.getResult();
	}

	@SuppressWarnings("serial")
	private static class ParsingStateException extends ScriptException {

		public ParsingStateException(String message, Throwable cause) {
			super(message, cause);
		}

		public ParsingStateException(String message) {
			super(message);
		}

	}

	private interface ParsingState {

		void processToken(ParsingMachine context) throws ParsingStateException;

	}

	private interface NextStateProvider {

		ParsingState getNextState(ParsingMachine context);

	}

	private class StartState implements ParsingState {

		public void processToken(ParsingMachine context) throws ParsingStateException {
			String token = context.getNextToken();
			StringBuilder javaExpression = context.getJavaExpression();
			javaExpression.append("return");

			ParsingUtil.FieldInfo fldInfo;

			if (NumberUtil.isInteger(token)) {
				javaExpression.append(" ").append(ParsingUtil.INTEGER_VALUEOF_EXPR).append(token);
				context.setState(context.INTEGER_STATE);
			} else if (NumberUtil.isDouble(token)) {
				javaExpression.append(" ").append(ParsingUtil.DOUBLE_VALUEOF_EXPR).append(token);
				context.setState(context.DOUBLE_STATE);
			} else if (BooleanUtil.isBoolean(token)) {
				javaExpression.append(" ").append(ParsingUtil.BOOLEAN_VALUEOF_EXPR).append(token);
				context.setState(context.BOOLEAN_STATE);
			} else if (ParsingUtil.isStringLiteral(token)) {
				javaExpression.append(" ").append(ParsingUtil.LP);
				javaExpression.append(ParsingUtil.toJavaStringExpr(token));
				context.setState(context.STRING_STATE);
			} else if (ParsingUtil.isIntegerVariable(token, context.getScriptContext())) {
				javaExpression.append(" ").append(ParsingUtil.INTEGER_VALUEOF_EXPR);
				javaExpression.append(String.format(ParsingUtil.INTEGER_VAR_EXPR_TEMPLATE, token));
				context.setState(context.INTEGER_STATE);
			} else if (ParsingUtil.isIntegerField(token, context.getScriptContext(),
					fldInfo = new ParsingUtil.FieldInfo())) {
				javaExpression.append(" ").append(ParsingUtil.INTEGER_VALUEOF_EXPR);
				String fieldExpr = String.format(ParsingUtil.INTEGER_FLD_EXPR_TEMPLATE, fldInfo.varType,
						fldInfo.varName, fldInfo.fieldName);
				javaExpression.append(fieldExpr);
				context.setState(context.INTEGER_STATE);
			} else if (ParsingUtil.isDoubleVariable(token, context.getScriptContext())) {
				javaExpression.append(" ").append(ParsingUtil.DOUBLE_VALUEOF_EXPR);
				javaExpression.append(String.format(ParsingUtil.DOUBLE_VAR_EXPR_TEMPLATE, token));
				context.setState(context.DOUBLE_STATE);
			} else if (ParsingUtil.isDoubleField(token, context.getScriptContext(),
					fldInfo = new ParsingUtil.FieldInfo())) {
				javaExpression.append(" ").append(ParsingUtil.DOUBLE_VALUEOF_EXPR);
				String fieldExpr = String.format(ParsingUtil.DOUBLE_FLD_EXPR_TEMPLATE, fldInfo.varType, fldInfo.varName,
						fldInfo.fieldName);
				javaExpression.append(fieldExpr);
				context.setState(context.DOUBLE_STATE);
			} else if (ParsingUtil.isBooleanVariable(token, context.getScriptContext())) {
				javaExpression.append(" ").append(ParsingUtil.BOOLEAN_VALUEOF_EXPR);
				javaExpression.append(String.format(ParsingUtil.BOOLEAN_VAR_EXPR_TEMPLATE, token));
				context.setState(context.BOOLEAN_STATE);
			} else if (ParsingUtil.isBooleanField(token, context.getScriptContext(),
					fldInfo = new ParsingUtil.FieldInfo())) {
				javaExpression.append(" ").append(ParsingUtil.BOOLEAN_VALUEOF_EXPR);
				String fieldExpr = String.format(ParsingUtil.BOOLEAN_FLD_EXPR_TEMPLATE, fldInfo.varType,
						fldInfo.varName, fldInfo.fieldName);
				javaExpression.append(fieldExpr);
				context.setState(context.BOOLEAN_STATE);
			} else if (ParsingUtil.isStringVariable(token, context.getScriptContext())) {
				javaExpression.append(" ").append(ParsingUtil.LP);
				javaExpression.append(String.format(ParsingUtil.STRING_VAR_EXPR_TEMPLATE, token));
				context.setState(context.STRING_STATE);
			} else if (ParsingUtil.isStringField(token, context.getScriptContext(),
					fldInfo = new ParsingUtil.FieldInfo())) {
				javaExpression.append(" ").append(ParsingUtil.LP);
				String fieldExpr = String.format(ParsingUtil.STRING_FLD_EXPR_TEMPLATE, fldInfo.varType, fldInfo.varName,
						fldInfo.fieldName);
				javaExpression.append(fieldExpr);
				context.setState(context.STRING_STATE);
			} else {
				throw new ParsingStateException("Unexpected token [" + token + "]");
			}

			context.process();
		}

	}

	private class EndState implements ParsingState {

		public void processToken(ParsingMachine context) throws ParsingStateException {
			context.getJavaExpression().append(";");
		}

	}

	private abstract class ValueState implements ParsingState, NextStateProvider {

		public void processToken(ParsingMachine context) throws ParsingStateException {
			StringBuilder javaExpression = context.getJavaExpression();

			if (context.hasNextToken()) {
				String token = context.getNextToken();
				if (isProperOperand(token)) {
					javaExpression.append(" ").append(token);
					context.setState(getNextState(context));
				} else {
					throw new ParsingStateException(
							"Operand is expected after [" + context.getCurrentToken() + "], but was [" + token + "]");
				}
			} else {
				javaExpression.append(ParsingUtil.RP);
				context.setState(context.END_STATE);
			}

			context.process();
		}

		protected abstract boolean isProperOperand(String token);
	}

	private abstract class OperandState implements ParsingState, NextStateProvider {

		public void processToken(ParsingMachine context) throws ParsingStateException {
			StringBuilder javaExpression = context.getJavaExpression();
			ParsingUtil.FieldInfo fldInfo = new ParsingUtil.FieldInfo();

			if (context.hasNextToken()) {
				String token = context.getNextToken();
				if (isProperPrimitiveType(token)) {
					javaExpression.append(" ").append(preprocessPrimitiveValueExpression(token));
				} else if (isProperVariableType(token, context.getScriptContext())) {
					javaExpression.append(" ");
					javaExpression.append(String.format(getVarExpressionTemplate(), token));
				} else if (isProperFieldType(token, context.getScriptContext(), fldInfo)) {
					String fieldExpr = String.format(getFieldExpressionTemplate(), fldInfo.varType, fldInfo.varName,
							fldInfo.fieldName);
					javaExpression.append(" ").append(fieldExpr);
				} else {
					throw new ParsingStateException(getTypeName() + " value or variable is expected after operand ["
							+ context.getCurrentToken() + "], but was [" + token + "]");
				}
				if (context.hasNextToken()) {
					context.setState(getNextState(context));
				} else {
					javaExpression.append(ParsingUtil.RP);
					context.setState(context.END_STATE);
				}
				context.process();
			} else {
				throw new ParsingStateException("Unexpected operand [" + context.getCurrentToken() + "] at the end");
			}

		}

		protected String preprocessPrimitiveValueExpression(String expr) {
			return expr;
		}

		protected abstract boolean isProperPrimitiveType(String token);

		protected abstract boolean isProperVariableType(String token, ScriptContext scriptContext);

		protected abstract boolean isProperFieldType(String token, ScriptContext scriptContext,
				ParsingUtil.FieldInfo fieldInfo);

		protected abstract String getVarExpressionTemplate();

		protected abstract String getFieldExpressionTemplate();

		protected abstract String getTypeName();

	}

	private class IntegerState extends ValueState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.INT_OPERAND_STATE;
		}

		@Override
		protected boolean isProperOperand(String token) {
			return ParsingUtil.NUMERIC_OPERANDS.contains(token);
		}
	}

	private class IntegerOperandState extends OperandState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.INTEGER_STATE;
		}

		@Override
		protected boolean isProperPrimitiveType(String token) {
			return NumberUtil.isInteger(token);
		}

		@Override
		protected boolean isProperVariableType(String token, ScriptContext scriptContext) {
			return ParsingUtil.isIntegerVariable(token, scriptContext);
		}

		@Override
		protected String getVarExpressionTemplate() {
			return ParsingUtil.INTEGER_VAR_EXPR_TEMPLATE;
		}

		@Override
		protected String getTypeName() {
			return ParsingUtil.INTEGER_TYPE_ALIAS;
		}

		@Override
		protected boolean isProperFieldType(String token, ScriptContext scriptContext,
				ParsingUtil.FieldInfo fieldInfo) {

			return ParsingUtil.isIntegerField(token, scriptContext, fieldInfo);
		}

		@Override
		protected String getFieldExpressionTemplate() {
			return ParsingUtil.INTEGER_FLD_EXPR_TEMPLATE;
		}

	}

	private class DoubleState extends ValueState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.DBL_OPERAND_STATE;
		}

		@Override
		protected boolean isProperOperand(String token) {
			return ParsingUtil.NUMERIC_OPERANDS.contains(token);
		}

	}

	private class DoubleOperandState extends OperandState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.DOUBLE_STATE;
		}

		@Override
		protected boolean isProperPrimitiveType(String token) {
			return NumberUtil.isDouble(token);
		}

		@Override
		protected boolean isProperVariableType(String token, ScriptContext scriptContext) {
			return ParsingUtil.isDoubleVariable(token, scriptContext);
		}

		@Override
		protected String getVarExpressionTemplate() {
			return ParsingUtil.DOUBLE_VAR_EXPR_TEMPLATE;
		}

		@Override
		protected String getTypeName() {
			return ParsingUtil.DOUBLE_TYPE_ALIAS;
		}

		@Override
		protected boolean isProperFieldType(String token, ScriptContext scriptContext,
				ParsingUtil.FieldInfo fieldInfo) {

			return ParsingUtil.isDoubleField(token, scriptContext, fieldInfo);
		}

		@Override
		protected String getFieldExpressionTemplate() {
			return ParsingUtil.DOUBLE_FLD_EXPR_TEMPLATE;
		}

	}

	private class BooleanState extends ValueState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.BOOL_OPERAND_STATE;
		}

		@Override
		protected boolean isProperOperand(String token) {
			return ParsingUtil.BOOLEAN_OPERANDS.contains(token);
		}

	}

	private class BooleanOperandState extends OperandState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.BOOLEAN_STATE;
		}

		@Override
		protected boolean isProperPrimitiveType(String token) {
			return BooleanUtil.isBoolean(token);
		}

		@Override
		protected boolean isProperVariableType(String token, ScriptContext scriptContext) {
			return ParsingUtil.isBooleanVariable(token, scriptContext);
		}

		@Override
		protected boolean isProperFieldType(String token, ScriptContext scriptContext,
				ParsingUtil.FieldInfo fieldInfo) {

			return ParsingUtil.isBooleanField(token, scriptContext, fieldInfo);
		}

		@Override
		protected String getVarExpressionTemplate() {
			return ParsingUtil.BOOLEAN_VAR_EXPR_TEMPLATE;
		}

		@Override
		protected String getFieldExpressionTemplate() {
			return ParsingUtil.BOOLEAN_FLD_EXPR_TEMPLATE;
		}

		@Override
		protected String getTypeName() {
			return ParsingUtil.BOOLEAN_TYPE_ALIAS;
		}

	}

	private class StringState extends ValueState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.STR_OPERAND_STATE;
		}

		@Override
		protected boolean isProperOperand(String token) {
			return ParsingUtil.STRING_OPERANDS.contains(token);
		}

	}

	private class StringOperandState extends OperandState {

		@Override
		public ParsingState getNextState(ParsingMachine context) {
			return context.STRING_STATE;
		}

		@Override
		protected boolean isProperPrimitiveType(String token) {
			return ParsingUtil.isStringLiteral(token);
		}

		@Override
		protected boolean isProperVariableType(String token, ScriptContext scriptContext) {
			return ParsingUtil.isStringVariable(token, scriptContext);
		}

		@Override
		protected boolean isProperFieldType(String token, ScriptContext scriptContext,
				ParsingUtil.FieldInfo fieldInfo) {
			return ParsingUtil.isStringField(token, scriptContext, fieldInfo);
		}

		@Override
		protected String getVarExpressionTemplate() {
			return ParsingUtil.STRING_VAR_EXPR_TEMPLATE;
		}

		@Override
		protected String getFieldExpressionTemplate() {
			return ParsingUtil.STRING_FLD_EXPR_TEMPLATE;
		}

		@Override
		protected String getTypeName() {
			return ParsingUtil.STRING_TYPE_ALIAS;
		}

		@Override
		protected String preprocessPrimitiveValueExpression(String expr) {
			return ParsingUtil.toJavaStringExpr(expr);
		}

	}

	private class ParsingMachine {

		private ParsingState state;

		private String[] tokens;

		private ScriptContext scriptContext;

		private int length;

		private int index;

		private StringBuilder javaExpression;

		private String currentToken;

		final StartState START_STATE = new StartState();
		final IntegerState INTEGER_STATE = new IntegerState();
		final IntegerOperandState INT_OPERAND_STATE = new IntegerOperandState();
		final DoubleState DOUBLE_STATE = new DoubleState();
		final DoubleOperandState DBL_OPERAND_STATE = new DoubleOperandState();
		final BooleanState BOOLEAN_STATE = new BooleanState();
		final BooleanOperandState BOOL_OPERAND_STATE = new BooleanOperandState();
		final StringState STRING_STATE = new StringState();
		final StringOperandState STR_OPERAND_STATE = new StringOperandState();
		final EndState END_STATE = new EndState();

		public ParsingMachine(String scriptExpression, ScriptContext scriptContext) {
			tokens = ParsingUtil.escapeStringLiteralBlanks(scriptExpression.trim()).split(ParsingUtil.BLANK_REGEX);
			this.scriptContext = scriptContext;
			length = tokens.length;
			javaExpression = new StringBuilder();

			index = -1;
			state = START_STATE;
		}

		void setState(ParsingState state) {
			this.state = state;
		}

		StringBuilder getJavaExpression() {
			return javaExpression;
		}

		String getNextToken() {
			return tokens[++index];
		}

		String getCurrentToken() {
			return currentToken;
		}

		boolean hasNextToken() {
			return index < length - 1;
		}

		ScriptContext getScriptContext() {
			return scriptContext;
		}

		public void process() throws ParsingStateException {
			if (index > -1) {
				currentToken = tokens[index];
			}
			state.processToken(this);
		}

		public String getResult() {
			return javaExpression.toString();
		}

	}

	private static class ParsingUtil {

		private static final String BLANK_CODE = "%20%";

		private static final String BLANK = " ";

		private static final String BLANK_REGEX = "\\s+";

		private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'.*?'");

		private static final String DQ = "\"";

		private static final List<String> NUMERIC_OPERANDS = Arrays.asList(new String[] { "+", "-", "/", "*" });

		private static final List<String> BOOLEAN_OPERANDS = Arrays.asList(new String[] { "&&", "||" });

		private static final List<String> STRING_OPERANDS = Arrays.asList(new String[] { "+" });

		private static final String INTEGER_VALUEOF_EXPR = "Integer.valueOf(";

		private static final String DOUBLE_VALUEOF_EXPR = "Double.valueOf(";

		private static final String BOOLEAN_VALUEOF_EXPR = "Boolean.valueOf(";

		private static final String LP = "(";

		private static final String RP = ")";

		private static final String SQ = "'";

		private static final String INTEGER_VAR_EXPR_TEMPLATE = "((Integer)scriptContext.getVar(\"%s\").getValue()).intValue()";

		private static final String INTEGER_FLD_EXPR_TEMPLATE = "((%s)scriptContext.getVar(\"%s\").getValue()).%s.intValue()";

		private static final String DOUBLE_VAR_EXPR_TEMPLATE = "((Double)scriptContext.getVar(\"%s\").getValue()).doubleValue()";

		private static final String DOUBLE_FLD_EXPR_TEMPLATE = "((%s)scriptContext.getVar(\"%s\").getValue()).%s.doubleValue()";

		private static final String BOOLEAN_VAR_EXPR_TEMPLATE = "((Boolean)scriptContext.getVar(\"%s\").getValue()).booleanValue()";

		private static final String BOOLEAN_FLD_EXPR_TEMPLATE = "((%s)scriptContext.getVar(\"%s\").getValue()).%s.booleanValue()";

		private static final String STRING_VAR_EXPR_TEMPLATE = "(String)scriptContext.getVar(\"%s\").getValue()";

		private static final String STRING_FLD_EXPR_TEMPLATE = "((%s)scriptContext.getVar(\"%s\").getValue()).%s";

		private static final String INTEGER_TYPE_ALIAS = "Integer";

		private static final String DOUBLE_TYPE_ALIAS = "Decimal";

		private static final String BOOLEAN_TYPE_ALIAS = "Boolean";

		private static final String STRING_TYPE_ALIAS = "String";

		static boolean isIntegerVariable(String token, ScriptContext context) {
			return isVariable(token, context, Integer.class);
		}

		static boolean isIntegerField(String token, ScriptContext context, FieldInfo info) {
			return isField(token, context, Integer.class, info);
		}

		static boolean isDoubleVariable(String token, ScriptContext context) {
			return isVariable(token, context, Double.class);
		}

		static boolean isDoubleField(String token, ScriptContext context, FieldInfo info) {
			return isField(token, context, Double.class, info);
		}

		static boolean isBooleanVariable(String token, ScriptContext context) {
			return isVariable(token, context, Boolean.class);
		}

		static boolean isBooleanField(String token, ScriptContext context, FieldInfo info) {
			return isField(token, context, Boolean.class, info);
		}

		static boolean isStringVariable(String token, ScriptContext context) {
			return isVariable(token, context, String.class);
		}

		static boolean isStringField(String token, ScriptContext context, FieldInfo info) {
			return isField(token, context, String.class, info);
		}

		static boolean isStringLiteral(String token) {
			return token.length() > 1 && token.startsWith(SQ) && token.endsWith(SQ);
		}

		static String toJavaStringExpr(String stringLiteral) {
			final String stringWoSingleQuotes = stringLiteral.substring(1, stringLiteral.length() - 1);

			return new StringBuilder().append(DQ).append(stringWoSingleQuotes.replace(BLANK_CODE, BLANK)).append(DQ)
					.toString();
		}

		// String literals with blanks are escaped: ' abc ' -> '%20%abc%20'. It is needed for splitting expression into
		// tokens. The splitting regex is "\\s+".
		static String escapeStringLiteralBlanks(final String scriptExpression) {
			Matcher matcher = STRING_LITERAL_PATTERN.matcher(scriptExpression);
			String escapedExpression = scriptExpression;

			while (matcher.find()) {
				final String literal = matcher.group();
				if (literal.contains(BLANK)) {
					final String escapedLiteral = literal.replace(BLANK, BLANK_CODE);
					escapedExpression = escapedExpression.replace(literal, escapedLiteral);
				}
			}

			return escapedExpression;
		}

		private static boolean isField(String token, ScriptContext context, Class<?> fieldType, FieldInfo info) {
			// Expect Variable.Field expression
			String[] parts = token.split("\\.");
			if (parts.length != 2) {
				return false;
			}

			String varName = parts[0];

			if (isVariable(varName, context)) {
				Variable var = context.getVar(varName);
				Class<?> varClass = resolveClass(var.getJavaType());
				String fieldName = parts[1];
				if (varClass != null && isPublicField(varClass, fieldName, fieldType)) {
					info.varName = varName;
					info.fieldName = fieldName;
					info.varType = varClass.getName();

					return true;
				}
			}

			return false;
		}

		private static boolean isVariable(String token, ScriptContext context) {
			return !Character.isDigit(token.charAt(0)) && StringUtils.isAlphanumeric(token)
					&& (context.getVar(token) != null);
		}

		private static boolean isVariable(String token, ScriptContext context, Class<?> varType) {
			boolean hasProperFormat = !Character.isDigit(token.charAt(0)) && StringUtils.isAlphanumeric(token);
			if (!hasProperFormat) {
				return false;
			}

			Variable var = context.getVar(token);
			if (var == null) {
				return false;
			}

			if (var.getJavaType().equals(varType.getName())) {
				return true;
			} else {
				return false;
			}
		}

		private static boolean isPublicField(Class<?> clazz, String fieldName, Class<?> fieldType) {
			Field fld = FieldUtils.getField(clazz, fieldName);
			return fld != null && fld.getType() == fieldType;
		}

		private static Class<?> resolveClass(String name) {
			Class<?> clazz;
			try {
				clazz = DynamicClassLoader.getInstance().loadClass(name);
			} catch (ClassNotFoundException e) {
				return null;
			}
			return clazz;
		}

		static class FieldInfo {
			String varName;
			String fieldName;
			String varType;
		}
	}

}
