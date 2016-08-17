package com.ilsid.bfa.graph;

import java.util.HashMap;
import java.util.Map;

import com.ilsid.bfa.flow.FlowConstants;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

/**
 * Converts script's graph representation into <a href="http://flowchart.js.org">flowchart.js</a> format.
 * 
 * @author illia.sydorovych
 *
 */
public class FlowChartJsAdapter {

	private static final String LF = "\n";

	private static final String VERTEX_DEF_SEPARATOR = "=>";

	private static final String EDGE_ARROW = "->";

	private static final String COLON = ": ";

	private static final char LB = '(';

	private static final char RB = ')';

	private static final Map<String, String> DEF_TYPE_MAP;

	private static final Map<String, String> LABEL_MAP;

	static {
		DEF_TYPE_MAP = new HashMap<>();
		DEF_TYPE_MAP.put(FlowConstants.START, "start");
		DEF_TYPE_MAP.put(FlowConstants.END, "end");
		DEF_TYPE_MAP.put(FlowConstants.OPERATION, "operation");
		DEF_TYPE_MAP.put(FlowConstants.CONDITION, "condition");
		DEF_TYPE_MAP.put(FlowConstants.SUBFLOW, "subroutine");

		LABEL_MAP = new HashMap<>();
		LABEL_MAP.put(FlowConstants.YES_LABEL, "yes");
		LABEL_MAP.put(FlowConstants.NO_LABEL, "no");
	}

	/**
	 * Build script's flow representation
	 * 
	 * @param source
	 *            script's source code
	 * @return flowchart.js representation of script's flow.
	 */
	public static String buildFlowChart(String source) {
		Graph graph = FlowGraphBuilder.buildGraph(source);
		String flowChartJsCode = convertToFlowChartJs(graph);

		return flowChartJsCode;
	}

	private static String convertToFlowChartJs(Graph graph) {
		StringBuilder lines = new StringBuilder();

		for (Vertex vertex : GraphUtil.getSortedByIdVertices(graph)) {
			StringBuilder vertexDef = new StringBuilder();
			String name = vertex.getProperty(FlowConstants.NAME_PROPERTY);
			String type = vertex.getProperty(FlowConstants.TYPE_PROPERTY);

			vertexDef.append(vertex.getId()).append(VERTEX_DEF_SEPARATOR).append(DEF_TYPE_MAP.get(type)).append(COLON)
					.append(name);

			lines.append(vertexDef.toString()).append(LF);
		}

		lines.append(LF);

		for (Edge edge : GraphUtil.getSortedByIdEdges(graph)) {
			StringBuilder edgeDef = new StringBuilder();

			edgeDef.append(edge.getVertex(Direction.OUT).getId());

			String label = LABEL_MAP.get(edge.getLabel());
			if (label != null) {
				edgeDef.append(LB).append(label).append(RB);
			}

			edgeDef.append(EDGE_ARROW).append(edge.getVertex(Direction.IN).getId());

			lines.append(edgeDef).append(LF);
		}

		return lines.toString();
	}

}
