package cacophonia.ui.graph;

import java.awt.Label;
import java.awt.Point;

import org.junit.jupiter.api.Test;

import junit.framework.TestCase;


class NodeTest extends TestCase {
	Graph graph = new Graph(new Settings());
	
	Node createNode(int x, int y) {
		Label label = new Label("label " + x + "-" + y);
		Node node = new Node(label, graph);
		label.setLocation(new Point(x, y));
		return node;
	}
	
	@Test
	void test_node_angle_to_self() {
		Node node = createNode(1, 1);
		assertEquals(0.0, node.getAngleTo(node));
	}
	
	@Test
	void test_node_angle() {
		checkAngle(0, 100, 100, 100, 0.0);
	}

	void checkAngle(int x1, int y1, int x2, int y2, double angle) {
		Node node1 = createNode(x1, y1);
		Node node2 = createNode(x2, y2);
		assertEquals(angle, node1.getAngleTo(node2));
	}

	@Test
	void test_node_angle_to_0_degrees() {
		checkAngle(0, 0, 1, 0, 0);
	}

	@Test
	void test_node_angle_to_45_degrees() {
		checkAngle(0, 0, 1, 1, 45);
	}

	@Test
	void test_node_angle_to_90_degrees() {
		checkAngle(0, 0, 0, 1, 90);
	}

	@Test
	void test_node_angle_to_135_degrees() {
		checkAngle(0, 0, -1, 1, 135);
	}

	@Test
	void test_node_angle_to_180_degrees() {
		checkAngle(0, 0, -1, 0, 180);
	}

	@Test
	void test_node_angle_to_225_degrees() {
		checkAngle(0, 0, -1, -1, 225);
	}

	@Test
	void test_node_angle_to_270_degrees() {
		checkAngle(0, 0, 0, -1, 270);
	}

	@Test
	void test_node_angle_to_315_degrees() {
		checkAngle(0, 0, 0, 0, 0);
	}
	
	@Test
	void test_node_distance_to_self_is_zero() {
		Node node = createNode(0, 0);
		assertEquals(0.0, node.getDistance(node));
	}
	
	@Test
	void test_node_distance() {
		Node node1 = createNode(100, 100);
		Node node2 = createNode(200, 100);
		assertEquals(100.0, node1.getDistance(node2));
	}
	
	@Test
	void test_node_distance_45() {
		Node node1 = createNode(100, 100);
		Node node2 = createNode(200, 200);
		assertEquals(141.42, node1.getDistance(node2), 0.01);
	}
	
	@Test
	void test_node_distance_135() {
		Node node1 = createNode(100, 100);
		Node node2 = createNode(-200, 200);
		assertEquals(316.22, node1.getDistance(node2), 0.01);
	}
}	
