package cacophonia.ui.graph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Label;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


public class Graph extends JPanel {

	public Settings settings;

	private Map<Component,Node> nameToNodes;
	private List<Node> nodes;
	private Map<Edge,Integer> edgeToIndex;
	private List<Edge> edges;
	private Node center;
	private Graphics2D imageGraphics;
	private BufferedImage image;
	private boolean running = false;
	private Node selectedNode;
	private List<PopupMenuListener> popupMenuListeners = new ArrayList<>();
	private List<PaintListener> paintListeners = new ArrayList<>();
	private List<SelectionListener> selectionListeners = new ArrayList<>();
	private double layoutDamage = 0;
	private Container header;


	public Graph(Settings settings) {
		this.settings = settings;
		edges = new ArrayList<>();
		edgeToIndex = new HashMap<>();
		nodes = new ArrayList<>();
		nameToNodes = new HashMap<>();
		center = new Node(new Label("center"), this);
		center.component.setLocation(new Point(settings.width/2, settings.height/2));
		setLocation(0, 0);
		setSize(new Dimension(settings.width, settings.height));
		setupListeners();
		addControls();
		addDebugToggle();
	}

	void start() {
		if (running) return;
		running = true;
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						layoutGraph();
						Thread.sleep((long)Math.max(layoutDamage, settings.layoutDelay));
						layoutDamage = 0;
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}).start();
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						repaint();
						header.repaint();
						Thread.sleep(settings.redrawDelay);
						layoutDamage = 0;
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}).start();
	}

	public void clear() {
		edges.clear();
		edgeToIndex.clear();
		nodes.clear();
		nameToNodes.clear();
	}

	public void paintComponent(Graphics g) {
		paintComponent((Graphics2D)g);
	}
	
	public void paintComponent(Graphics2D g) {
		super.paintComponent(g);
		for (PaintListener listener: paintListeners) {
			listener.paintBefore(imageGraphics);
		}
		paintWholeGraph(imageGraphics);
		for (PaintListener listener: paintListeners) {
			listener.paintAfter(imageGraphics);
		}
		g.drawImage(image, 0, 0, this);
	}	

	void paintWholeGraph(Graphics2D g) {
		g.setColor(Color.BLACK);
		g.fillRect(0, 50, settings.width, settings.height);
		Edge.paintAll(this, g);
		Node.paintAll(this, g);
	}

	public Node addNode(Component component) {
		if (!nameToNodes.containsKey(component)) {
			Node node = new Node(component, this);
			nameToNodes.put(component, node);
			nodes.add(node);
			layoutDamage += settings.layoutDamageNewNode;
		}
		return nameToNodes.get(component);
	}
	
	public void addCallEdge(Component from, Component to) {
		Node fromNode = getNode(from);
		Node toNode = getNode(to);
		addEdge(fromNode, toNode,
				settings.historyEdgeWeight,
				settings.historyEdgeDecay, 
				settings.historyEdgeLength,
				settings.historyEdgeAttractionForce,
				settings.historyEdgeLevel,
				settings.historyEdgeColor
		);
		addEdge(fromNode, toNode,
				settings.callEdgeWeight,
				settings.callEdgeDecay, 
				settings.callEdgeLength,
				settings.callEdgeAttractionForce,
				settings.callEdgeLevel,
				settings.callEdgeColor
		);
		start();
	}
	
	public Edge addEdge(Node from, Node to, double weight, double decay, double length, double force, int level, Color color) {
		Edge edge = new Edge(from, to, weight, decay, length, force, level, color, this);
		if (edgeToIndex.containsKey(edge)) {
			try {
				edges.set(edgeToIndex.get(edge), edge);
			} catch (IndexOutOfBoundsException e) {
				// this happens when the graph is cleared while new edges arrive
			}
		} else {
			edgeToIndex.put(edge, edges.size());
			edges.add(edge);
		}
		layoutDamage += settings.layoutDamageCall;
		return edge;
	}

	public void layoutGraph() {
		for (Node node: nodes) {
			node.reset();
		}
		List<Node> sortedNodes = getNodes();
		for (int n=0; n<settings.maxLayoutIterationCount; n++) {
			for (Node node: sortedNodes) {
				node.repulseFromOtherNodes();
				node.attractToCenter(center);
				node.dampen();
			}
			for (Edge edge: getEdges()) {
				edge.attractNodes();
			}
		}
		for (Node node: sortedNodes) {
			node.applyCalculatedForces();
			node.decay();
		}
	}

	public List<Edge> getEdges() {
		return new ArrayList<>(edges);
	}

	List<Node> getNodes() {
		List<Node> copy = new ArrayList<Node>(nodes)
				.stream()
				.filter(node -> node.age > 0)
				.collect(Collectors.toList());
		copy.sort(Comparator.comparing(Node::getAge));
		return copy;
	}

	void addControls() {
		header = new Container();
		header.setLayout(new FlowLayout());
		add(header);
		addRepelForceSlider();
		addCallForceSlider();
		addRelatedForceSlider();
		addHistoryForceSlider();
	}

	void removeControls() {
		if (header != null) remove(header);
	}

	void addCallForceSlider() {
		header.add(new Label("call:"));
		JSlider slider = new JSlider(0, 200, (int)(100.0 * settings.callEdgeAttractionForce));
		slider.setBackground(Color.white);
		slider.setSize(200, 25);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settings.callEdgeAttractionForce = (double)slider.getValue() / 100.0;
				System.out.println(String.format("Call force: %s", settings.callEdgeAttractionForce));
			}
		});
		header.add(slider);
	}

	void addHistoryForceSlider() {
		header.add(new Label("history:"));
		JSlider slider = new JSlider(0, 200, (int)(100.0 * settings.historyEdgeAttractionForce));
		slider.setBackground(Color.white);
		slider.setSize(200, 25);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settings.historyEdgeAttractionForce = (double)slider.getValue() / 100.0;
				System.out.println(String.format("History force: %s", settings.historyEdgeAttractionForce));
			}
		});
		header.add(slider);
	}

	void addRelatedForceSlider() {
		header.add(new Label("related:"));
		JSlider slider = new JSlider(0, 200, (int)(100.0 * settings.relatedEdgeAttractionForce));
		slider.setBackground(Color.white);
		slider.setSize(200, 25);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settings.relatedEdgeAttractionForce = (double)slider.getValue() / 100.0;
				System.out.println(String.format("Related force: %s", settings.relatedEdgeAttractionForce));
			}
		});
		header.add(slider);
	}

	void addDebugToggle() {
		JCheckBox checkbox = new JCheckBox("debug", false);
		checkbox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settings.debug = !settings.debug;
			}
		});
		header.add(checkbox);
	}

	void addRepelForceSlider() {
		header.add(new Label("repel:"));
		JSlider slider = new JSlider(0, 40000, (int)settings.repulsionForce);
		slider.setBackground(Color.white);
		slider.setSize(200, 25);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settings.repulsionForce = (double)slider.getValue();
				System.out.println(String.format("Repel force: %s", settings.repulsionForce));
			}
		});
		header.add(slider);
	}

	public Node getNode(Component component) {
		return addNode(component);
	}

	public void updateAge(Component component, double age) {
		getNode(component).setAge(age);
	}
	
	Node findNode(int x, int y) {
		for (int n=nodes.size() - 1; n>=0; n--) {
			Node node = nodes.get(n);
			if (node.getAge() >= settings.defaultAge - 1 && node.contains(x, y)) {
				return node;
			}
		}
		return null;
	}
	
	public void addPopupMenuListener(PopupMenuListener listener) {
		popupMenuListeners.add(listener);
	}
	
	public void addPaintListener(PaintListener listener) {
		paintListeners.add(listener);
	}
	
	public void addSelectionListener(SelectionListener listener) {
		selectionListeners.add(listener);
	}
	
	void setupListeners() {
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				image = new BufferedImage(settings.width, settings.height, BufferedImage.TYPE_INT_ARGB);
				imageGraphics = (Graphics2D) image.getGraphics();
				super.componentResized(e);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				selectedNode = findNode(e.getX(), e.getY());
				if (selectedNode != null) {
					selectedNode.mousePressed(e.getX(), e.getY());
					for (SelectionListener listener: selectionListeners) {
						listener.select(selectedNode.component);
					}
				}
				super.mousePressed(e);
			}
			
	    	public void mouseClicked(MouseEvent e) {
				Node node = findNode(e.getX(), e.getY());
	    		if (node == null) return;
	    		switch (e.getButton()) {
	    		case 1:
	    			node.locationFixed = !node.locationFixed;
	    			break;
	    		case 3:
					for (PopupMenuListener listener: popupMenuListeners) {
						PopupMenu menu = listener.createMenu(node.component);
						add(menu);
						menu.show(Graph.this, e.getX(), e.getY());
					}
	    			break;
	    		}
				super.mouseClicked(e);
	    	}
		});

		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				if (selectedNode != null) {
					selectedNode.mouseDragged(e.getX(), e.getY());
				}
			}
			@Override
			public void mouseMoved(MouseEvent e) {
				for (Node node : nodes) {
					if (node.getAge() > 0 && node.contains(e.getX(), e.getY())) {
						node.mouseMoved(e.getX(), e.getY());
					}
				}
				super.mouseMoved(e);
			}
		});
	}
	
}
