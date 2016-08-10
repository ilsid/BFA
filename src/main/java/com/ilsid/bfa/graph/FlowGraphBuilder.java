package com.ilsid.bfa.graph;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.ilsid.bfa.common.ClassNameUtil;
import com.ilsid.bfa.flow.FlowConstants;
import com.ilsid.bfa.flow.FlowElement;
import com.ilsid.bfa.script.CompilerConstants;
import com.ilsid.bfa.script.Script;
import com.ilsid.bfa.script.ScriptSourcePreprocessor;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;

/**
 * Builds a graph representation of a scripting flow.
 * 
 * @author illia.sydorovych
 *
 */
public class FlowGraphBuilder {

	/**
	 * Builds graph for a given script.
	 * 
	 * @param source
	 *            script source code
	 * @return flow graph
	 */
	public static Graph buildGraph(String source) {
		final String fullSource = String.format(CompilerConstants.SCRIPT_SOURCE_TEMPLATE,
				ClassNameUtil.TMP_PACKAGE_NAME, ClassNameUtil.TMP_CLASS_NAME, source);
		final String preprocessedSource = ScriptSourcePreprocessor.processVarargs(fullSource);

		CompilationUnit compilationUnit;
		try {
			try (InputStream scriptSource = IOUtils.toInputStream(preprocessedSource);) {
				compilationUnit = JavaParser.parse(scriptSource);
			}
		} catch (ParseException | IOException e) {
			throw new IllegalStateException("Failed to parse script", e);
		}

		VisitorContext visitorContext = new VisitorContext();
		new GraphVisitor().visit(compilationUnit, visitorContext);

		return visitorContext.graph;
	}

	private static class GraphVisitor extends VoidVisitorAdapter<VisitorContext> {

		private static final Pattern DESCRIPTION_PLACEHOLDER_PATTERN = Pattern.compile("(%[0-9]{1}+)");

		@Override
		public void visit(MethodCallExpr m, VisitorContext visitorContext) {
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

					for (Annotation a : parentMethod.getDeclaredAnnotations()) {
						if (a.annotationType() == FlowElement.class) {
							Vertex vertex = visitorContext.graph.addVertex(null);
							FlowElement flowElement = (FlowElement) a;
							vertex.setProperty(FlowConstants.TYPE_PROPERTY, flowElement.type());
							String elementName = determineElementName(flowElement.description(), m);
							vertex.setProperty(FlowConstants.NAME_PROPERTY, elementName);
							System.out.println(vertex.getProperty(FlowConstants.TYPE_PROPERTY) + " | "
									+ vertex.getProperty(FlowConstants.NAME_PROPERTY));
							break;
						}
					}

					break;
				}
			}
		}

		private String determineElementName(String elementDescription, MethodCallExpr methodExpr) {
			Expression[] methodParams = methodExpr.getArgs().toArray(new Expression[] {});
			Matcher matcher = DESCRIPTION_PLACEHOLDER_PATTERN.matcher(elementDescription);
			String name = elementDescription;

			while (matcher.find()) {
				String placeHolder = matcher.group();
				int paramIdx = Integer.parseInt(placeHolder.substring(1));
				String value = ((StringLiteralExpr) methodParams[paramIdx]).getValue();
				name = name.replace(placeHolder, value);
			}

			return name;

		}
	}

	private static class VisitorContext {

		Graph graph = new TinkerGraph();

	}

}
