package cacophonia.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cacophonia.Constants;
import cacophonia.DetailLevel;
import cacophonia.runtime.Util;
import cacophonia.ui.graph.Graph;
import cacophonia.ui.graph.Node;
import cacophonia.ui.graph.PaintListener;
import cacophonia.ui.graph.SelectionListener;
import cacophonia.ui.graph.Vector;

/**
 * Represents a plugin. Its name will be abbreviated. Each plugin has its own location in the canvas it is drawn on.
 * Each plugin has a unique instrument and tone used to generate sound.
 */
public class Plugin extends Component {
	static HashMap<String,Plugin>plugins = new HashMap<>();
	static Composite opaque = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
	static Composite transparancies[] = {
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f),
	};
	static double currentScale = 1;
	static Font fonts[] = getFonts();
	static Color colors[] = {
			new Color(204, 133, 154), // pink
			new Color(144, 201, 120), // pistachio
			new Color(255, 249, 170), // calamanci
			new Color(131, 198, 221), // dark sky blue
			new Color(255, 185, 179), // melon
			new Color(210, 145, 188)  // violet
	};
	static Font statisticsFont = new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE);
	static Map<String,Color> filterColors = new HashMap<>();
	static DetailLevel detailLevel = DetailLevel.FEATURE;
	static Plugin selectedPlugin;
	static SelectionListener selectionListener;
	static PaintListener paintListener;


	Set<String> relatedPluginNames = new HashSet<String>();
	String[] names;
	String fullName, name;
	int instrument, note;
	boolean beingInspected;
	Color focusColor;
	int dx, dy;
	Node node;
	boolean noteOn;

	
	public Plugin(String name, Graph graph) {
		this.fullName = name;
		this.name = name.replace("_feature", "").replace("_plugin", "");
		node = graph.addNode(this);
		names = this.name
				.replace("com.", "")
				.replace("org.", "")
				.replace("eclipse.", "")
				.replace(".feature", "")
				.split("\\.");
		if (UI.currentSoundTheme != null) {
			instrument = UI.currentSoundTheme.getInstrument(name);
			chooseNote();
		}
		setSize(Constants.PLUGIN_SIZE, Constants.PLUGIN_SIZE);
		setFocus();
		setRelated(graph);
		setupListeners(graph);
	}

	void setupListeners(Graph graph) {
		setupSelectionListener(graph);
		setupPaintListener(graph);
	}

	void setupSelectionListener(Graph graph) {
		if (selectionListener != null) return;
		selectionListener = new SelectionListener() {
			@Override
			public void select(Component component) {
				selectedPlugin = (Plugin) component;
				UI.selectInstrument(selectedPlugin.instrument);
			}
		};
		graph.addSelectionListener(selectionListener);
	}

	void setupPaintListener(Graph graph) {
		if (paintListener != null) return;
		paintListener = new PaintListener() {
			@Override
			public void paintBefore(Graphics2D g) {
			}
			@Override
			public void paintAfter(Graphics2D g) {
				drawStatistics(g);
			}
		};
		graph.addPaintListener(paintListener);
	}

	void setRelated(Graph graph) {
		if (detailLevel == DetailLevel.FRAGMENT) {
			relatedPluginNames = PluginRegistry.getFamilyForFragment(name);
		}
		if (detailLevel == DetailLevel.PLUGIN) {
			relatedPluginNames = PluginRegistry.getFamilyForPlugin(name);
		}
		for (String name: relatedPluginNames) {
			if (!plugins.containsKey(name)) continue;
			Plugin relatedPlugin = plugins.get(name);
			graph.addEdge(this.node, relatedPlugin.node,
				graph.settings.relatedEdgeWeight,
				graph.settings.relatedEdgeDecay, 
				graph.settings.relatedEdgeLength,
				graph.settings.relatedEdgeAttractionForce,
				graph.settings.relatedEdgeLevel,
				graph.settings.relatedEdgeColor
			);
		}
		String[] filters = UI.pluginFilter.split(",");
		for (String filter: filters) {
			if (filter.length() > 0 && name.indexOf(filter) != -1) {
				for (Plugin other: plugins.values()) {
					if (other == this) continue;
					if (other.name.indexOf(filter) != -1) {
						graph.addEdge(this.node, other.node,
							graph.settings.relatedEdgeWeight,
							graph.settings.relatedEdgeDecay, 
							graph.settings.relatedEdgeLength,
							graph.settings.relatedEdgeAttractionForce,
							graph.settings.relatedEdgeLevel,
							graph.settings.relatedEdgeColor
						);
					}
				}
			};
		}
	}

	@Override
	public String toString() {
		return String.format("Plugin[%s,%d,%d]", name, getX(), getY());
	}

	public void setInstrument(int instrument) {
		this.instrument = instrument;
		UI.currentSoundTheme.setInstrument(name, instrument);
	}

	public static void clearInstruments() {
		for (Plugin plugin: plugins.values()) {
			plugin.instrument = -1;
		}
	}
	
	public static void clear() {		
		synchronized (plugins) {
			for (Plugin plugin: plugins.values()) {
				plugin.setInspect(false);
			}
			plugins.clear();
			selectedPlugin = null;
		}
	}

	public static void updateInstruments() {
		for (Plugin plugin: plugins.values()) {
			plugin.instrument = UI.currentSoundTheme.getInstrument(plugin.name);
		}
	}

	void chooseNote() {
		note = UI.currentSoundTheme.getNote(name);
	}
	
	static void focusUpdated() {
		for (Plugin plugin : plugins.values()) {
			plugin.setFocus();
		}
	}
	
	void setFocus() {
		focusColor = null;
		if (UI.pluginFilter == "") return;
		String[] filters = UI.pluginFilter.split(",");
		for (String filter: filters) {
			if (filter.length() > 0 && name.indexOf(filter) != -1) {
				focusColor = getFocusColor(filter);
			};
		}
	}
	
	Color getFocusColor(String filter) {
		if (!filterColors.containsKey(filter)) {
			Color color = colors[ filterColors.size() % colors.length ];
			filterColors.put(filter, color);
		}
		return filterColors.get(filter);
	}

	static void drawStatistics(Graphics2D g) {
		String maxMemory = Util.formatSize(Runtime.getRuntime().maxMemory());
		String totalMemory = Util.formatSize(Runtime.getRuntime().totalMemory());
		String myMemory = String.format("Cacophonia: memory=%s/%s", totalMemory, maxMemory);
		g.setFont(statisticsFont);
		g.setColor(Color.YELLOW);
		g.setComposite(Plugin.opaque);
		g.drawString("Eclipse:    " + UI.statistics, 10, Constants.HEIGHT - 85);
		g.drawString(myMemory, 10, Constants.HEIGHT - 70);
	}
	
	double getAge() {
		return node.getAge();
	}

	public static Plugin get(String name, Graph graph) {
		Plugin plugin = plugins.get(name);
		if (plugin == null) {
			plugin = new Plugin(name, graph);
			synchronized (plugins) {
				plugins.put(name,  plugin);
			}
		}
		return plugin;
	}

	public boolean asleep() {
		return getAge() < 0;
	}
	
	@Override
	public Rectangle getBounds() {
		Rectangle area = super.getBounds();
		int size = getPluginSize();
		area.width = size;
		area.height = size;
		return area;
	}
	
	int getPluginOffset() {
		return (int)(3 * (Constants.HEART_BEAT - getAge()));
	}
	
	int getPluginSize() {
		return (int)(currentScale * Constants.PLUGIN_SIZE - 2 * getPluginOffset());
	}

	public void paint(java.awt.Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		if (node.graph.settings.debug) {
			g.setColor(Color.WHITE);
			g.drawString(name, getX(), getY());
		}
		if (node == null || getAge() < 0) return;
		setTransparancy(g);
		setBorderWidth(g);
		setBackgroundColor(g);
		int offset = getPluginOffset();
		int x = getX() + offset;
		int y = getY() + offset;
		int size = getPluginSize();
		g.fillOval(x, y, size, size);
		setBorderColor(g);
		g.drawOval(x, y, size, size);
		setTextColor(g);
		setFont(g);
		g.setClip(getX(), getY(), size, size);
		int fontWidth = (int)(8 * currentScale);
		int fontHeight = (int)(11 * currentScale);
		int yOffset = (size / fontHeight - names.length) / 2 * fontHeight;
		for (int n=0; n<names.length; n++) {
			int xOffset = (int)(size / 2 - fontWidth * names[n].length() / 2);
			g.drawString(names[n], x + xOffset, y + yOffset + (n+1) * fontHeight);
		}
		g.setClip(null);
		if (instrument != -1) {
			setNumberColor(g);
			g.drawString(String.format("%d", instrument), x, y);
		}
		if (beingInspected) {
			setInspectColor(g);
			g.setStroke(new BasicStroke(2));
			g.fillOval(x + 15, y + size - 13, 10, 10);
		}
		if (node.locationFixed) {
			g.setColor(Color.RED);
			g.setStroke(new BasicStroke(2));
			g.drawString("#", x - 2, y + 2 + size);
		}
		if (node.graph.settings.debug) {
			setNumberColor(g);
			g.drawString(String.format("f=%.3f a=%.1f", node.vector.force, node.vector.angle), x, y-5);
			g.setColor(Color.RED);
			g.setStroke(new BasicStroke(2));
			int vx = (int)node.vector.getX(x);
			int vy = (int)node.vector.getY(y);
			g.drawLine(x, y, vx, vy);
			g.setStroke(new BasicStroke(1));
			g.drawOval(vx - 2, vy - 2, 5, 5);
		}
	}
	
	public void setTransparancy(Graphics2D g) {
		Composite transparancy = opaque;
		if (focusColor == null) {
			transparancy = transparancies[Math.max(1, Math.min(transparancies.length - 1, (int)getAge()))];
		}
		g.setComposite(transparancy);
	}
	
	public void setFont(Graphics2D g) {
		g.setFont(fonts[(int)getAge()]);
	}
	
	void setBackgroundColor(Graphics2D g) {
		g.setColor(focusColor != null ? focusColor : Color.DARK_GRAY);
	}
	
	void setInspectColor(Graphics2D g) {
		g.setColor(focusColor != null ? Color.RED : Color.YELLOW);
	}
	
	void setBorderWidth(Graphics2D g) {
		g.setStroke(new BasicStroke(selectedPlugin == this || focusColor != null ? 5 : getAge() < 2 ? 1 : 2));
	}
	
	void setBorderColor(Graphics2D g) {
		g.setColor(detailLevel == DetailLevel.FRAGMENT ? Color.ORANGE : detailLevel == DetailLevel.FEATURE ? Color.WHITE : Color.GREEN);
	}
	
	void setTextColor(Graphics2D g) {
		g.setColor(focusColor != null ? Color.BLACK : Color.ORANGE);
	}

	void setNumberColor(Graphics2D g) {
		g.setColor(Color.WHITE);
	}

	void beep() {
		if (instrument == -1 || UI.muted || noteOn) return;
		UI.orchestra.setInstrument(instrument);
		UI.orchestra.noteOn(note);
		noteOn = true;
		new Thread(new Runnable() {
			public void run() {	
				try { Thread.sleep(Constants.MUTE_DELAY); } catch (Exception e) { }
				UI.orchestra.setInstrument(instrument);
				UI.orchestra.noteOff(note);
				noteOn = false;
			}
		}).start();
	}

	public void toggleInspect() {
		setInspect(!beingInspected);
	}

	public void setInspect(boolean inspect) {
		if (beingInspected == inspect) return;
		beingInspected = inspect;
		Set<String> fragments = PluginRegistry.getAllFragments(fullName, detailLevel);
		String names = String.join(" ", fragments);
		UI.sendEvent(beingInspected ? Constants.EVENT_INSPECT_PLUGIN : Constants.EVENT_UN_INSPECT_PLUGIN, names);
	}

	public static void called(String fromName, String toName, Graph graph) {
		Plugin from = get(fromName, graph);
		Plugin to = get(toName, graph);
		graph.addCallEdge(from, to);
		graph.updateAge(from, Constants.MAX_AGE);
		graph.updateAge(to, Constants.MAX_AGE);
		from.beep();
		to.beep();
	}
	
	@Override
	public String getName() {
		return fullName;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Plugin && ((Plugin)other).name.equals(name);
	}

	public static void setScale(double scale) {
		currentScale = scale;
		fonts = getFonts();
	}
	
	static Font[] getFonts() {
		return new Font[] {
		    new Font("Courier New", Font.PLAIN, (int)(currentScale * Constants.FONT_SIZE - 6)),
		    new Font("Courier New", Font.PLAIN, (int)(currentScale * Constants.FONT_SIZE - 3)),
		    new Font("Courier New", Font.PLAIN, (int)(currentScale * Constants.FONT_SIZE)),
		    new Font("Courier New", Font.PLAIN, (int)(currentScale * Constants.FONT_SIZE)),
		    new Font("Courier New", Font.PLAIN, (int)(currentScale * Constants.FONT_SIZE)),
		    new Font("Courier New", Font.PLAIN, (int)(currentScale * Constants.FONT_SIZE))
		};
	}

}