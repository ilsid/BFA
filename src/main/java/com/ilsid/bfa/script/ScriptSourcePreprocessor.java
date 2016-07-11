package com.ilsid.bfa.script;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.DumpVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.ilsid.bfa.persistence.DynamicClassLoader;

/**
 * Prepares script for compilation stage.
 * 
 * @author illia.sydorovych
 *
 */
class ScriptSourcePreprocessor {

	private static final String TMP_CLASS_NAME = "TmpScript";

	private static final String TMP_PACKAGE_NAME = "com.ilsid.bfa.tmp";

	private static final String LB_PATTERN = "[\r\n]";

	private static final String EMPTY = "";

	private static final String SEMICOLON = ";";

	private static final String COMMA = ",";

	private static final String RB = ")";

	private static final String RCB = "}";

	private static final String OBJECT_ARRAY_TEMPLATE = " new Object[] { %s }";

	private static final Pattern ACTION_OR_SUBFLOW_WITH_PARAMS_PATTERN = Pattern
			.compile(".*(Action\\(.+?,.+?\\)|SubFlow\\(.+?,.+?\\)).*", Pattern.DOTALL);

	/**
	 * This is a work-around for Javassist limitation. Javassist does not support Java <i>varargs</i>. Calls of
	 * {@link Script#Action(String, Object...)} methods are replaced with explicit array arguments. <br/>
	 * For example, <br/>
	 * <br/>
	 * <code>Action("Some Action", "Var1", "Var2");</code> <br/>
	 * <br/>
	 * is replaced with <br/>
	 * <br/>
	 * <code>Action("Some Action", new Object[] {  "Var1", "Var2" });</code>
	 * 
	 * <br/>
	 * <br/>
	 * The same pre-processing is applied for {@link Script#SubFlow(String, Object...)}. <br/>
	 * See also <a href=
	 * "Javassist Tutorial" >http://jboss-javassist.github.io/javassist/tutorial/tutorial3.html#varargs</a>.
	 * 
	 * @param source
	 *            input source code
	 * @return the source code without <i>varargs</i> method calls.
	 */
	public static String processVarargs(String source) {
		String[] expressions = source.split(SEMICOLON);
		StringBuilder output = new StringBuilder();

		for (String expr : expressions) {
			Matcher matcher = ACTION_OR_SUBFLOW_WITH_PARAMS_PATTERN.matcher(expr);
			if (matcher.matches()) {
				String actionExpr = matcher.group(1);
				int offset = expr.indexOf(actionExpr);
				final int commaIdx = offset + actionExpr.indexOf(COMMA);
				final int closeBracketIdx = offset + actionExpr.indexOf(RB);
				String varargsExpr = expr.substring(commaIdx + 1, closeBracketIdx);
				String actionWithArrayParamExpr = expr.substring(0, commaIdx + 1)
						+ String.format(OBJECT_ARRAY_TEMPLATE, varargsExpr) + expr.substring(closeBracketIdx)
						+ SEMICOLON;
				output.append(actionWithArrayParamExpr);
			} else {
				output.append(expr);
				final String trimmedExpr = expr.replaceAll(LB_PATTERN, EMPTY).trim();
				if (trimmedExpr.length() > 0 && !trimmedExpr.endsWith(RCB)) {
					output.append(SEMICOLON);
				}
			}
		}

		return output.toString();
	}

	/**
	 * Replaces scripting expressions with java code. Scripting expressions are string literals. The replacement java
	 * code provides actual values for corresponding expressions in runtime.
	 * 
	 * @param source
	 *            original script code
	 * @return a unit that contains the transformed script code (with replaced expressions) and optional input
	 *         parameters. Note, the transformed code contains enclosing brackets.In case of expressions parsing errors,
	 *         a list of corresponding exceptions is returned within a unit. The script code is <code>null</code> and
	 *         input parameters are empty in case of parsing errors.
	 * @throws ParsingException
	 *             if the passed script code is invalid
	 */
	public static ExpressionsUnit processExpressions(String source) throws ParsingException {
		final String fullSource = String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE, TMP_PACKAGE_NAME,
				TMP_CLASS_NAME, source);

		CompilationUnit compilationUnit;
		try {
			try (InputStream scriptSource = IOUtils.toInputStream(fullSource);) {
				compilationUnit = JavaParser.parse(scriptSource);
			}
		} catch (ParseException e) {
			throw new ParsingException("Invalid script source code", e);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read script source", e);
		}

		final MethodVisitorContext visitorContext = new MethodVisitorContext();
		// 1st AST traversal: mark script expressions (modify script body) and map them to java code
		new ScriptExpressionsVisitor().visit(compilationUnit, visitorContext);

		ExpressionsUnit result = new ExpressionsUnit();

		if (visitorContext.exceptions.isEmpty()) {
			// 2nd AST traversal: extract modified script body with marked expressions
			new ScriptBodyVisitor().visit(compilationUnit, visitorContext);

			final Map<String, String> parsedExpressions = visitorContext.parsedExpressions;
			String resultSource = visitorContext.scriptBody;

			// Replace marked script expressions with mapped java code
			for (String replaceToken : parsedExpressions.keySet()) {
				resultSource = StringUtils.replace(resultSource, replaceToken, parsedExpressions.get(replaceToken));
			}

			result.source = resultSource;
			result.inputParameters = new LinkedHashMap<>(visitorContext.scriptInputParameters);
		} else {
			result.processingErrors.addAll(visitorContext.exceptions);
		}

		return result;
	}

	static class ExpressionsUnit {

		private String source;

		private Map<String, String> inputParameters = new LinkedHashMap<>();

		private List<Exception> processingErrors = new LinkedList<>();

		public String getSource() {
			return source;
		}

		public Map<String, String> getInputParameters() {
			return inputParameters;
		}

		public List<Exception> getProcessingErrors() {
			return processingErrors;
		}

	}

	private static class MethodVisitorContext {

		ScriptContext scriptContext = new ScriptContext();

		ScriptExpressionParser parser = new ScriptExpressionParser(scriptContext);

		Map<String, String> scriptInputParameters = new LinkedHashMap<>();

		List<Exception> exceptions = new LinkedList<>();

		Map<String, String> parsedExpressions = new LinkedHashMap<>();

		String scriptBody;
	}

	private static class ScriptExpressionsVisitor extends VoidVisitorAdapter<MethodVisitorContext> {

		private static final String DQ = "\"";

		private static final String EXPRESSION_PREFIX = "@@EXPR@@";

		@Override
		public void visit(MethodCallExpr m, MethodVisitorContext visitorContext) {
			for (Node child : m.getChildrenNodes()) {
				child.accept(this, visitorContext);
			}

			Expression[] methodParams = m.getArgs().toArray(new Expression[] {});
			List<Method> parentMethods = new LinkedList<>();
			parentMethods.addAll(Arrays.asList(Script.class.getMethods()));
			parentMethods.addAll(Arrays.asList(Script.ActionResult.class.getMethods()));

			for (Method parentMethod : parentMethods) {
				if (m.getName().equals(parentMethod.getName())
						&& methodParams.length == parentMethod.getParameterTypes().length) {

					Var varAnnotation = parentMethod.getAnnotation(Var.class);
					if (varAnnotation != null) {
						processVariableDeclaration(methodParams, varAnnotation, visitorContext);
					}

					int paramIdx = 0;
					for (Annotation[] annotations : parentMethod.getParameterAnnotations()) {
						for (Annotation a : annotations) {
							if (a.annotationType() == ExprParam.class) {
								processExpression(methodParams[paramIdx], visitorContext,
										((ExprParam) a).replaceOnCompile());
							}
						}
						paramIdx++;
					}
					break;
				}
			}

		}

		private void processVariableDeclaration(Expression[] methodParams, Var varAnnotation,
				MethodVisitorContext visitorContext) {
			// Method with Var annotation must have at least two string
			// parameters: name and type
			String varName = ((StringLiteralExpr) methodParams[0]).getValue();
			String varType = ((StringLiteralExpr) methodParams[1]).getValue();

			String javaType = TypeNameResolver.resolveEntityClassName(varType);
			checkVarType(varName, varType, javaType, visitorContext);

			ScriptContext scriptContext = visitorContext.scriptContext;

			// Variables are saved in the script context for the further
			// expressions parsing stage
			try {
				if (varAnnotation.scope() == Var.Scope.LOCAL) {
					Object varValue = null;
					if (methodParams.length > 2) {
						varValue = extractValue(methodParams[2]);
					}
					scriptContext.addLocalVar(varName, javaType, varValue);
				} else {
					visitorContext.scriptInputParameters.put(varName, varType);
					scriptContext.addInputVar(varName, javaType, null);
				}
			} catch (ScriptException e) {
				visitorContext.exceptions.add(e);
			}
		}

		private Object extractValue(Expression expression) throws ScriptException {
			Object result;

			if (StringLiteralExpr.class.isInstance(expression)) {
				result = ((StringLiteralExpr) expression).getValue();
			} else {
				throw new ScriptException(String.format("Unexpected expression [%s]", expression));
			}

			return result;
		}

		private void checkVarType(String varName, String varType, String javaType,
				MethodVisitorContext visitorContext) {
			try {
				DynamicClassLoader.getInstance().loadClass(javaType);
			} catch (ClassNotFoundException e) {
				Exception ce = new ClassCompilationException(
						String.format("Variable [%s] has invalid type [%s]", varName, varType));
				visitorContext.exceptions.add(ce);
			}
		}

		private void processExpression(Node expression, MethodVisitorContext visitorContext, boolean replaceIsNeeded) {
			if (StringLiteralExpr.class.isInstance(expression)) {
				final StringLiteralExpr stringLiteralExpr = (StringLiteralExpr) expression;
				String scriptExpr = stringLiteralExpr.getValue();
				Map<String, String> parsedExpressions = visitorContext.parsedExpressions;

				try {
					String javaExpr = visitorContext.parser.parse(scriptExpr);

					if (replaceIsNeeded) {
						final String replacementExpr = EXPRESSION_PREFIX + scriptExpr;
						stringLiteralExpr.setValue(replacementExpr);
						parsedExpressions.put(DQ + replacementExpr + DQ, javaExpr);
					}
				} catch (ParsingException e) {
					visitorContext.exceptions.add(e);
				}
			} else if (ArrayCreationExpr.class.isInstance(expression)) {
				// This is a case for varargs that have been replaced with explicit array statement
				for (Node childNode : expression.getChildrenNodes()) {
					for (Node grandChildNode : childNode.getChildrenNodes()) {
						// The string literals with method parameter values are the grand-children of "Create Array"
						// expression
						processExpression(grandChildNode, visitorContext, true);
					}
				}
			}
		}

	}

	private static class ScriptBodyVisitor extends VoidVisitorAdapter<MethodVisitorContext> {

		@Override
		public void visit(MethodDeclaration m, MethodVisitorContext visitorContext) {
			if (m.getName().equals(CompilerConstants.SCRIPT_IMPLEMENTATION_METHOD_NAME)) {
				final DumpVisitor dumpVisitor = new DumpVisitor();
				dumpVisitor.visit(m.getBody(), null);
				visitorContext.scriptBody = dumpVisitor.getSource();
			}
		}

	}

}
