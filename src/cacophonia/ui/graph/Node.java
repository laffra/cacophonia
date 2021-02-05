package cacophonia.ui.graph;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class Node {
	static Preferences preferences = Preferences.userNodeForPackage(Node.class);

	public Graph graph;
	public Component component;
	public double age;
	public double damping; 
	public Vector vector = new Vector(0.0, 0.0);
	public Point mouseOffset = new Point();
	public boolean locationFixed;

	Node(Component component, Graph graph) {
		this.component = component;
		this.graph = graph;
		setInitialLocation();
	}

	public void setInitialLocation() {
		String xy[] = preferences.get("position-" + component.getName(), "0 0").split(" ");
		int x = Integer.parseInt(xy[0]);
		int y = Integer.parseInt(xy[1]);
		if (x != 0 && y != 0) {
			locationFixed = true;
		} else {
			x = (int)(graph.settings.width/2 - 100 + Math.random() * 200);
			y = (int)(graph.settings.height/2 - 100 + Math.random() * 200);
		}
		component.setLocation(x, y);
	}

	double getDistance(Node other) {
		return getDistance(other.getX(), other.getY());
	}
	
	double getDistance(int x, int y) {
		return Math.sqrt(Math.pow(getX() - x, 2) + Math.pow(getY() - y, 2));
	}
	
	@Override
	public String toString() {
		return String.format("Node[%s,age=%.1f,%s]", component, getAge(), vector);
	}
	
	void reset() {
		vector.reset();
		damping = 1;
	}

	void repulseFromOtherNodes() {
		for (Node other: graph.getNodes()) {
			double minAge = Math.min(getAge(), other.getAge());
			if (other == this || minAge < 1) continue;
			double distance = Math.max(getDistance(other), 30);
			double force = -(graph.settings.repulsionForce / Math.pow(distance, 2));
			force *= minAge / graph.settings.defaultAge;
			vector.add(new Vector(damping * force, getAngleTo(other)));
		}
	}

	void attractToCenter(Node center) {
		if (getAge() < 1) return;
		double distance = Math.max(getDistance(center), 1);
		double force = Math.min(1, graph.settings.centerAttractForce / Math.pow(2 * distance, 2));
		if (distance - force < graph.settings.centerMinDistance) return;
		vector.add(new Vector(damping * force, getAngleTo(center)));
	}
	
	double getAngleTo(int x, int y) {
		double angle = Math.toDegrees(Math.atan2(y - getY(), x - getX()));
		return angle < 0 ? angle + 360 : angle;
	}
	
	double getAngleTo(Node other) {
		return getAngleTo(other.getX(), other.getY());
	}
	
	void dampen() {
		damping /= 2;
	}
	
	void decay() {
		age -= graph.settings.ageDecay;
	}

	int getX() {
		return component.getX();
	}

	int getY() {
		return component.getY();
	}
	
	boolean contains(int x, int y) {
		Rectangle area = component.getBounds();
		x -= area.x;
		y -= area.y;
		return x > 0 && y > 0 && x < area.width && y < area.height;
	}

	void applyCalculatedForces() {
		if (locationFixed || vector.force == 0.0) return;
		moveTo((int)vector.getX(getX()), (int)vector.getY(getY()));
	}
	
	void moveTo(int x, int y) {
		int w = graph.settings.width;
		int h = graph.settings.height;
		int radius = graph.settings.averageNodeSize;
		x = Math.max(x, 5);
		y = Math.max(y, 5 + radius);
		x = Math.min(x, w - 2 * radius);
		y = Math.min(y, h - 3 * radius);
		component.setLocation(x, y);
	}

	public double getAge() {
		return age;
	}

	public void setAge(double age) {
		this.age = age;
	}
	
	@Override
	public int hashCode() {
		return component.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Node && ((Node)other).component == component;
	}

	public static void paintAll(Graph graph, Graphics2D graphics) {
		for (Node node: graph.getNodes()) {
			if (node.age > 0) {
				node.component.paint(graphics);
			}
		}
	}

	public void mousePressed(int x, int y) {
		mouseOffset.x = x - getX();
		mouseOffset.y = y - getY();
	}

	public void mouseDragged(int x, int y) {
		component.setLocation(x - mouseOffset.x, y - mouseOffset.y);
		locationFixed = true;
		preferences.put("position-" + component.getName(), String.format("%d %d", x, y));
	}

	public void mouseMoved(int x, int y) {
		age = graph.settings.defaultAge;
	}

	public void addForce(int dx, int dy) {
		if (age < 1) return;
		double force = getDistance(getX() + dx, getY() + dy);
		double angle = getAngleTo(getX() + dx, getY() + dy);
		vector.add(new Vector(force, angle));
	}
	
	public static void clear() {
		try {
			preferences.clear();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

}
	