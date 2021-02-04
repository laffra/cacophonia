package cacophonia.ui.graph;

import java.awt.Color;

public class Settings {

	double defaultAge = 5;
	double ageDecay = 0.05;
	int maxLayoutIterationCount = 10;
	long redrawDelay = 50;
	double layoutDamageCall = 0.002;
	double layoutDamageNewNode = 0.1;
	
	int width = 1200;
	int height = 1000;
	int averageNodeSize = 60;
	
	// settings for nodes to repel from each other
	int maxNodeRepelDistance = 90;
	double repulsionForce = 4000;
	double centerAttractForce = 4;
	double centerMinDistance = 150;
	double maxForce = 50;

	// settings for recent call edges to attract two nodes
	double callEdgeWeight = 9.0;
	double callEdgeDecay = 0.5;
	double callEdgeAttractionForce = 0.8;
	double callEdgeLength = 250;
	Color callEdgeColor = Color.WHITE;
	int callEdgeLevel = 3;

	// settings for calls in the past edges to attract two nodes
	double historyEdgeWeight = 5.0;
	double historyEdgeDecay = 0.05;
	double historyEdgeAttractionForce = 0.5;
	double historyEdgeLength = 350;
	Color historyEdgeColor = Color.PINK;
	int historyEdgeLevel = 1;

	// settings to cluster nodes that are related
	double familyEdgeWeight = 2.0;
	double familyEdgeDecay = Double.MIN_VALUE;
	double familyEdgeAttractionForce = 0.01;
	double familyEdgeLength = 250;
	Color familyEdgeColor = Color.BLUE;
	int familyEdgeLevel = 2;

}