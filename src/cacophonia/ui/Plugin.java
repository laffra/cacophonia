package cacophonia.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
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
	static Font fonts[] = {
		    new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE - 6),
		    new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE - 3),
		    new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE),
		    new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE),
		    new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE),
		    new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE)
	};
	static Color colors[] = {
			new Color(204, 133, 154), // pink
			new Color(144, 201, 120), // pistachio
			new Color(255, 249, 170), // calamanci
			new Color(131, 198, 221), // dark sky blue
			new Color(255, 185, 179), // melon
			new Color(210, 145, 188)  // violet
	};
	static Map<String,Color> filterColors = new HashMap<>();
	static boolean showJobs = false;
	static int pluginsShowing;
	static DetailLevel detailLevel = DetailLevel.FEATURE;
	static Plugin selectedPlugin;
	static SelectionListener selectionListener;
	static PaintListener paintListener;

	Set<String> family = new HashSet<String>();
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
		setFamily();
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

	void setFamily() {
		if (detailLevel == DetailLevel.FRAGMENT) {
			family = PluginRegistry.getFamilyForFragment(name);
		}
		if (detailLevel == DetailLevel.PLUGIN) {
			family = PluginRegistry.getFamilyForPlugin(name);
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
		g.setFont(fonts[fonts.length - 1]);
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

	public void paint(java.awt.Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		if (getAge() < 0) return;
		setTransparancy(g);
		setBorderWidth(g);
		setBackgroundColor(g);
		int adjust = (int)(3 * (Constants.HEART_BEAT - getAge()));
		int x = getX() + adjust;
		int y = getY() + adjust;
		int size = Constants.PLUGIN_SIZE - 2 * adjust;
		g.fillOval(x, y, size, size);
		setBorderColor(g);
		g.drawOval(x, y, size, size);
		setTextColor(g);
		setFont(g);
		g.setClip(getX(), getY(), Constants.PLUGIN_SIZE, Constants.PLUGIN_SIZE);
		int yOffset = (Constants.PLUGIN_SIZE / 11 - names.length) / 2 * 11;
		for (int n=0; n<names.length; n++) {
			int xOffset = size / 2 - 8 * names[n].length() / 2;
			g.drawString(names[n], x + xOffset, y + yOffset + (n+1) * 11);
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

}