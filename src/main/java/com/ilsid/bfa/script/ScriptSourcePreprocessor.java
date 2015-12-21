package com.ilsid.bfa.script;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-processes the script source code to be compilable with Javassist.
 * 
 * @author illia.sydorovych
 *
 */
class ScriptSourcePreprocessor {

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

}
