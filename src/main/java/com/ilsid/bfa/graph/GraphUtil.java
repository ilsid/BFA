package com.ilsid.bfa.graph;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

/**
 * Graph related utilities.
 * 
 * @author illia.sydorovych
 *
 */
public class GraphUtil {

	private static final IntegerIdComparator INTEGER_ID_COMPARATOR = new IntegerIdComparator();

	/**
	 * Returns vertices sorted by integer id's. Id's must be of integer type. Otherwise, behavior is not defined.
	 * 
	 * @param graph
	 * @return sorted vertices
	 */
	public static List<Vertex> getSortedByIdVertices(Graph graph) {
		List<Vertex> vertices = new LinkedList<>();
		for (Vertex vertex : graph.getVertices()) {
			vertices.add(vertex);
		}

		Collections.sort(vertices, INTEGER_ID_COMPARATOR);

		return vertices;
	}

	/**
	 * Returns edges sorted by integer id's. Id's must be of integer type. Otherwise, behavior is not defined.
	 * 
	 * @param graph
	 * @return sorted edges
	 */
	public static List<Edge> getSortedByIdEdges(Graph graph) {
		List<Edge> edges = new LinkedList<>();
		for (Edge edge : graph.getEdges()) {
			edges.add(edge);
		}

		Collections.sort(edges, INTEGER_ID_COMPARATOR);

		return edges;
	}

	private static class IntegerIdComparator implements Comparator<Element> {

		public int compare(Element elm1, Element elm2) {
			Integer id1 = Integer.parseInt(elm1.getId().toString());
			Integer id2 = Integer.parseInt(elm2.getId().toString());

			return id1.compareTo(id2);
		}

	}

}
