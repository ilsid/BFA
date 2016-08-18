package com.ilsid.bfa.graph;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.ilsid.bfa.BaseUnitTestCase;
import com.ilsid.bfa.common.IOHelper;
import com.ilsid.bfa.flow.FlowConstants;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

public class FlowGraphBuilderUnitTest extends BaseUnitTestCase {

	private Graph graph;

	// See src/test/resource/graph_builder_doc/flow-with-nested-condition.jpg for reference
	@Test
	public void graphForScriptWithSubConditionsCanBeBuilt() throws Exception {
		String source = IOHelper.loadScript("several-actions-and-subflows-with-params-script.txt");
		graph = FlowGraphBuilder.buildGraph(source);

		assertEquals(16, getCount(graph.getVertices()));
		assertEquals(18, getCount(graph.getEdges()));

		final String condition1 = "Var1 - Var3 == 1 ?";
		final String condition2 = "Var1 - Var3 == 2 ?";
		final String subCondition21 = "Var1 - Var3 == 5 ?";

		edge(FlowConstants.START, "Some Action 1");
		edge("Some Action 1", "Set Var3");
		edge("Set Var3", "Some Sub-Flow 1");
		edge("Some Sub-Flow 1", "Some Action 33");
		edge("Some Action 33", "Set Var4");
		edge("Set Var4", "Set Var5");
		edge("Set Var5", condition1);
		edge(condition1, "Some Action 2", FlowConstants.YES_LABEL);
		edge("Some Action 2", "Some Sub-Flow 2");
		edge("Some Sub-Flow 2", condition2);
		edge(condition1, condition2, FlowConstants.NO_LABEL);
		edge(condition2, "Some Action 3", FlowConstants.YES_LABEL);
		edge("Some Action 3", FlowConstants.END);
		edge(condition2, subCondition21, FlowConstants.NO_LABEL);
		edge(subCondition21, "Some Action 4", FlowConstants.YES_LABEL);
		edge("Some Action 4", "Some Action 5");
		edge("Some Action 5", FlowConstants.END);
		edge(subCondition21, FlowConstants.END, FlowConstants.NO_LABEL);
	}

	// See src/test/resource/graph_builder_doc/flow-with-composite-condition-stmt.jpg for reference
	@Test
	public void graphForScriptWithCompositeConditionStatementCanBeBuilt() throws Exception {
		String source = IOHelper.loadScript("composite-condition-script.txt");
		graph = FlowGraphBuilder.buildGraph(source);

		assertEquals(7, getCount(graph.getVertices()));
		assertEquals(7, getCount(graph.getEdges()));

		final String condition = "Var1 == 1 and Var2 == 33 or Var3 == 444 ?";

		edge(FlowConstants.START, "Set Var1 = 1");
		edge("Set Var1 = 1", "Set Var2 = 2.0");
		edge("Set Var2 = 2.0", condition);
		edge(condition, "Set Var3 = Var1", FlowConstants.YES_LABEL);
		edge(condition, "Set Var3 = 33", FlowConstants.NO_LABEL);
		edge("Set Var3 = Var1", FlowConstants.END);
		edge("Set Var3 = 33", FlowConstants.END);
	}

	private int getCount(Iterable<?> iterable) {
		int count = 0;
		for (Iterator<?> iterator = iterable.iterator(); iterator.hasNext();) {
			iterator.next();
			count++;
		}

		return count;
	}

	private Vertex getVertex(String value) {
		final String key = FlowConstants.NAME_PROPERTY;
		int cnt = 0;
		Vertex vertex = null;
		for (Iterator<Vertex> itr = graph.getVertices(key, value).iterator(); itr.hasNext();) {
			vertex = itr.next();
			cnt++;
			if (cnt > 1) {
				fail(String.format("More than one vertex with [%s = %s] found", key, value));
			}
		}

		if (cnt == 0) {
			fail(String.format("No vertex with [%s = %s] found", key, value));
		}

		return vertex;
	}

	private void edge(String outName, String inName) {
		edge(outName, inName, StringUtils.EMPTY);
	}

	private void edge(String outName, String inName, String label) {
		Vertex outVertex = getVertex(outName);
		Vertex inVertex = getVertex(inName);

		for (Edge edge : outVertex.getEdges(Direction.OUT, label)) {
			if (inVertex.equals(edge.getVertex(Direction.IN))) {
				return;
			}
		}
		fail(String.format("No edge with label [%s] exists from [%s] to [%s]", label,
				outVertex.getProperty(FlowConstants.NAME_PROPERTY), inVertex.getProperty(FlowConstants.NAME_PROPERTY)));
	}

}
