package com.ilsid.bfa.graph;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
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
		Vertex startVertex = context.graph.addVertex(++context.vertexCount);
		startVertex.setProperty(FlowConstants.NAME_PROPERTY, FlowConstants.START);
		startVertex.setProperty(FlowConstants.TYPE_PROPERTY, FlowConstants.START);
		context.outVertices.add(new OutVertex(startVertex, FlowConstants.EMPTY_LABEL));
	}

	private static void addEndVertex(GraphContext context) {
		Vertex endVertex = context.graph.addVertex(++context.vertexCount);
		endVertex.setProperty(FlowConstants.NAME_PROPERTY, FlowConstants.END);
		endVertex.setProperty(FlowConstants.TYPE_PROPERTY, FlowConstants.END);

		createEdgesToVertex(context, endVertex);
	}

	private static void createEdgesToVertex(GraphContext context, Vertex inVertex) {
		OutVertex outVertex;
		while ((outVertex = context.outVertices.poll()) != null) {
			context.graph.addEdge(++context.edgeCount, outVertex.instance, inVertex, outVertex.edgeLabel);
		}
	}

	private static class GraphVisitor extends VoidVisitorAdapter<GraphContext> {

		private static final Pattern DESCRIPTION_PLACEHOLDER_PATTERN = Pattern.compile("(%[0-9]{1}+)");

		private static final String QUESTION_MARK = "?";

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
							FlowElement flowElement = (FlowElement) a;
							String elementName = determineElementName(flowElement.description(), m);
							if (context.conditionState) {
								context.conditionNameParts.add(elementName);
							} else {
								Vertex newVertex = context.graph.addVertex(++context.vertexCount);
								newVertex.setProperty(FlowConstants.TYPE_PROPERTY, flowElement.type());
								newVertex.setProperty(FlowConstants.NAME_PROPERTY, elementName);

								createEdgesToVertex(context, newVertex);
								context.outVertices.add(new OutVertex(newVertex, FlowConstants.EMPTY_LABEL));
							}

							break;
						}
					}

					break;
				}
			}
		}

		@Override
		public void visit(IfStmt ifStmt, GraphContext context) {
			context.conditionState = true;
			ifStmt.getCondition().accept(this, context);
			context.conditionState = false;

			Vertex conditionVertex = createConditionVertex(context);
			context.outVertices.add(new OutVertex(conditionVertex, FlowConstants.YES_LABEL));

			ifStmt.getThenStmt().accept(this, context);

			context.outVertices.addFirst(new OutVertex(conditionVertex, FlowConstants.NO_LABEL));

			if (ifStmt.getElseStmt() != null) {
				OutVertex lastThenStmt = context.outVertices.removeLast();
				ifStmt.getElseStmt().accept(this, context);
				// outVerticies must contain last "then" statement as well after exiting "else" block
				context.outVertices.add(lastThenStmt);
			}
		}

		@Override
		public void visit(BinaryExpr bnrExpr, GraphContext context) {
			if (context.conditionState) {
				context.conditionOperators.add(bnrExpr.getOperator().name());
			}

			super.visit(bnrExpr, context);
		}

		private Vertex createConditionVertex(GraphContext context) {
			String namePart;
			StringBuilder fullName = new StringBuilder();
			while ((namePart = context.conditionNameParts.poll()) != null) {
				fullName.append(namePart).append(StringUtils.SPACE);
				String operator = context.conditionOperators.pollLast();
				if (operator != null) {
					fullName.append(operator).append(StringUtils.SPACE);
				}
			}
			fullName.append(QUESTION_MARK);

			Vertex conditionVertex = context.graph.addVertex(++context.vertexCount);
			conditionVertex.setProperty(FlowConstants.TYPE_PROPERTY, FlowConstants.CONDITION);
			conditionVertex.setProperty(FlowConstants.NAME_PROPERTY, fullName.toString());

			createEdgesToVertex(context, conditionVertex);

			return conditionVertex;
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

		boolean conditionState;

		Queue<String> conditionNameParts = new LinkedList<>();

		Deque<String> conditionOperators = new LinkedList<>();

		int vertexCount = 0;

		int edgeCount = 0;
	}

}
