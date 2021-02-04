package cacophonia.ui.graph;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;

class Edge {
	static int maxLevel;
	static Composite opaque = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
	static Composite transparancies[] = {
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f),
	};

	Node from, to;
	Graph graph;
	double weight;
	double decay;
	Color color;
	double length;
	double force;
	int level;

	Edge(Node from, Node to, double weight, double decay, double length, double force, int level, Color color, Graph graph) {
		this.from = from;
		this.to = to;
		this.weight = weight;
		this.color = color;
		this.graph = graph;
		this.decay = decay;
		this.force = force;
		this.length = length;
		this.level = level;
		maxLevel = Math.max(maxLevel, level);
	}

	public void paint(Graphics2D g) {
		if (weight < 1 || from.getAge() < 1 || to.getAge() < 1) return;
		Rectangle fromBounds = from.component.getBounds();
		Rectangle toBounds = to.component.getBounds();
		g.setColor(color);
		g.setStroke(new BasicStroke((int)weight));
		g.setComposite(transparancies[(int)weight]);
		g.drawLine(
			fromBounds.x + fromBounds.width/2,
			fromBounds.y + fromBounds.height/2,
			toBounds.x + toBounds.width/2,
			toBounds.y + toBounds.height/2
		);
		weight -= decay;
	}

	void attractNodes() {
		if (weight < 1) return;
		double distance = Math.max(1, from.getDistance(to));
		double force = this.force * Math.sqrt(Math.sqrt(distance));
		if (distance < length) return;
		from.vector.add(new Vector(from.damping * force, from.getAngleTo(to)));
		to.vector.add(new Vector(to.damping * force, to.getAngleTo(from)));
	}
	
	public static void paintAll(Graph graph, Graphics2D g) {
		for (int level = 1; level <= maxLevel; level++) {
			for (Edge edge: graph.getEdges()) {
				if (edge.level == level) {
					edge.paint(g);
				}
			}
		}
		g.setComposite(opaque);
	}
	
	@Override
	public int hashCode() {
		return from.hashCode() + to.hashCode() + color.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Edge && ((Edge)other).from == from &&
				((Edge)other).to == to && ((Edge)other).color == color;
	}

}
	