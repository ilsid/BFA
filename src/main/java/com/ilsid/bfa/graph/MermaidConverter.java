package com.ilsid.bfa.graph;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.flow.FlowConstants;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

/**
 * Converts script's graph representation into <a href="https://github.com/knsv/mermaid">mermaid</a> format.
 * 
 * @author illia.sydorovych
 *
 */
public class MermaidConverter {

	private static final String LF = "\n";

	private static final String TOP_BOTTOM_DIRECTION_EXPRESSION = "graph TB;";

	private static final String EDGE_ARROW = "-->";

	private static final String LABEL_SEPARATOR = "|";

	private static final Map<String, String> DEF_TYPE_MAP;

	private static final Map<String, String> LABEL_MAP;

	static {
		DEF_TYPE_MAP = new HashMap<>();
		DEF_TYPE_MAP.put(FlowConstants.START, "((Start))");
		DEF_TYPE_MAP.put(FlowConstants.END, "((End))");
		DEF_TYPE_MAP.put(FlowConstants.OPERATION, "[%s]");
		DEF_TYPE_MAP.put(FlowConstants.CONDITION, "{%s}");
		DEF_TYPE_MAP.put(FlowConstants.SUBFLOW, ">%s]");

		LABEL_MAP = new HashMap<>();
		LABEL_MAP.put(FlowConstants.YES_LABEL, "yes");
		LABEL_MAP.put(FlowConstants.NO_LABEL, "no");
	}

	/**
	 * Build script's flow representation.
	 * 
	 * @param source
	 *            script's source code
	 * @return mermaid representation of script's flow.
	 */
	public static String buildFlowChart(String source) {
		Graph graph = FlowGraphBuilder.buildGraph(source);
		String mermaidCode = convertToMermaid(graph);

		return mermaidCode;
	}

	private static String convertToMermaid(Graph graph) {
		StringBuilder lines = new StringBuilder();
		lines.append(TOP_BOTTOM_DIRECTION_EXPRESSION);
		lines.append(LF);

		for (Edge edge : GraphUtil.getSortedByIdEdges(graph)) {
			Vertex outVertex = edge.getVertex(Direction.OUT);
			Vertex inVertex = edge.getVertex(Direction.IN);
			String outName = outVertex.getProperty(FlowConstants.NAME_PROPERTY);
			String outType = outVertex.getProperty(FlowConstants.TYPE_PROPERTY);
			String inName = inVertex.getProperty(FlowConstants.NAME_PROPERTY);
			String inType = inVertex.getProperty(FlowConstants.TYPE_PROPERTY);

			StringBuilder edgeDef = new StringBuilder();

			edgeDef.append(outVertex.getId());
			edgeDef.append(String.format(DEF_TYPE_MAP.get(outType), outName));
			edgeDef.append(EDGE_ARROW);

			String label = LABEL_MAP.get(edge.getLabel());
			if (label != null) {
				edgeDef.append(LABEL_SEPARATOR).append(label).append(LABEL_SEPARATOR);
			}

			edgeDef.append(inVertex.getId());
			edgeDef.append(String.format(DEF_TYPE_MAP.get(inType), inName));

			lines.append(edgeDef).append(LF);
		}

		return lines.toString();
	}

}
