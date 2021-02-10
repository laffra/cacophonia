package cacophonia.ui.graph;

import java.awt.Color;

public class Settings {

	public boolean debug = false;
	public double defaultAge = 5;
	public double ageDecay = 0.1;
	public int maxLayoutIterationCount = 1;
	public long redrawDelay = 50;
	public long layoutDelay = 100;
	public double layoutDamageCall = 0.002;
	public double layoutDamageNewNode = 0.1;
	
	public int width = 1200;
	public int height = 1000;
	public int averageNodeSize = 60;
	
	// settings for nodes to repel from each other
	public int maxNodeRepelDistance = 90;
	public double repulsionForce = 13000;
	public double centerAttractForce = 4;
	public double centerMinDistance = 150;
	public double maxForce = 50;

	// settings for recent call edges to attract two nodes
	public double callEdgeWeight = 9.0;
	public double callEdgeDecay = 0.5;
	public double callEdgeAttractionForce = 0.5;
	public double callEdgeLength = 250;
	public Color callEdgeColor = Color.WHITE;
	public int callEdgeLevel = 3;

	// settings for calls in the past edges to attract two nodes
	public double historyEdgeWeight = 5.0;
	public double historyEdgeDecay = 0.05;
	public double historyEdgeAttractionForce = 0.2;
	public double historyEdgeLength = 350;
	public Color historyEdgeColor = Color.PINK;
	public int historyEdgeLevel = 1;

	// settings to cluster nodes that are related
	public double relatedEdgeWeight = 5.0;
	public double relatedEdgeDecay = 0;
	public double relatedEdgeAttractionForce = 1.5;
	public double relatedEdgeLength = 150;
	public Color relatedEdgeColor = Color.BLUE;
	public int relatedEdgeLevel = 2;

}