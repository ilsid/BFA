package com.ilsid.bfa.graph;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Deque;
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
import com.github.javaparser.ast.stmt.IfStmt;
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

	private static final String EMPTY_EDGE_LABEL = "";

	private static final String YES_EDGE_LABEL = "Yes";

	private static final String NO_EDGE_LABEL = "No";

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

		GraphContext context = new GraphContext();
		addStartVertex(context);
		new GraphVisitor().visit(compilationUnit, context);
		addEndVertex(context);

		return context.graph;
	}

	private static void addStartVertex(GraphContext context) {
		Vertex startVertex = context.graph.addVertex(null);
		startVertex.setProperty(FlowConstants.TYPE_PROPERTY, FlowConstants.START);
		context.outVertices.add(new OutVertex(startVertex, EMPTY_EDGE_LABEL));
	}

	private static void addEndVertex(GraphContext context) {
		Vertex endVertex = context.graph.addVertex(null);
		endVertex.setProperty(FlowConstants.TYPE_PROPERTY, FlowConstants.END);

		createEdgesToVertex(context, endVertex);
	}

	private static void createEdgesToVertex(GraphContext context, Vertex inVertex) {
		OutVertex outVertex;
		while ((outVertex = context.outVertices.poll()) != null) {
			context.graph.addEdge(null, outVertex.instance, inVertex, outVertex.edgeLabel);
		}
	}

	private static class GraphVisitor extends VoidVisitorAdapter<GraphContext> {

		private static final Pattern DESCRIPTION_PLACEHOLDER_PATTERN = Pattern.compile("(%[0-9]{1}+)");

		@Override
		public void visit(MethodCallExpr m, GraphContext context) {
			for (Node child : m.getChildrenNodes()) {
				child.accept(this, context);
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
							Vertex newVertex = context.graph.addVertex(null);
							FlowElement flowElement = (FlowElement) a;
							newVertex.setProperty(FlowConstants.TYPE_PROPERTY, flowElement.type());
							String elementName = determineElementName(flowElement.description(), m);
							newVertex.setProperty(FlowConstants.NAME_PROPERTY, elementName);
							System.out.println(newVertex.getProperty(FlowConstants.TYPE_PROPERTY) + " | "
									+ newVertex.getProperty(FlowConstants.NAME_PROPERTY));

							createEdgesToVertex(context, newVertex);
							context.outVertices.add(new OutVertex(newVertex, EMPTY_EDGE_LABEL));

							break;
						}
					}

					break;
				}
			}
		}

		@Override
		public void visit(IfStmt ifStmt, GraphContext context) {
			System.out.println();
			System.out.println("If Condition: " + ifStmt.getCondition() + " . Class: "
					+ ifStmt.getCondition().getClass().getName());
			// FIXME: Assumption is here that condition contains a single statement. For example such condition as
			// (Equal(...) && Equal(...)) breaks the logic
			ifStmt.getCondition().accept(this, context);
			OutVertex conditionStmt = context.outVertices.peek();
			conditionStmt.edgeLabel = YES_EDGE_LABEL;

			System.out.println("Then Statement: " + ifStmt.getThenStmt() + " . Class: "
					+ ifStmt.getThenStmt().getClass().getName());
			ifStmt.getThenStmt().accept(this, context);

			conditionStmt.edgeLabel = NO_EDGE_LABEL;
			context.outVertices.addFirst(conditionStmt);
			
			if (ifStmt.getElseStmt() != null) {
				System.out.println("Else Statement: " + ifStmt.getElseStmt() + " . Class: "
						+ ifStmt.getElseStmt().getClass().getName());
				OutVertex lastThenStmt = context.outVertices.removeLast();
				ifStmt.getElseStmt().accept(this, context);
				context.outVertices.add(lastThenStmt);
			} 
			System.out.println();
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

	private static class OutVertex {

		Vertex instance;

		String edgeLabel;

		public OutVertex(Vertex instance, String edgeLabel) {
			this.instance = instance;
			this.edgeLabel = edgeLabel;
		}

	}

	private static class GraphContext {

		Graph graph = new TinkerGraph();

		Deque<OutVertex> outVertices = new LinkedList<>();
	}

}
