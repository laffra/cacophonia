package cacophonia.ui.graph;

import java.awt.Label;

import org.junit.jupiter.api.Test;

import junit.framework.TestCase;


class GraphTest extends TestCase {
	@Test
	void test_node_distance() {
		Graph graph = new Graph(new Settings());
		Node node1 = new Node(new Label(), graph);
		assertEquals(node1.getDistance(node1), 0);
		Node node2 = new Node(new Label(), graph);
		assertEquals(node1.getDistance(node2), 0);
	}
	
}
