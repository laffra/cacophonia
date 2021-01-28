package cacophonia.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cacophonia.Constants;

/**
 * Represents a plugin. Its name will be abbreviated. Each plugin has its own location in the canvas it is drawn on.
 * Each plugin has a unique instrument and tone used to generate sound.
 */
class Plugin {
	static HashMap<String,Plugin>plugins = new HashMap<>();
	static Composite opaque = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
	static Composite transparancies[] = {
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f),
	};
	static Plugin selectedPlugin;
	static boolean useGraphLayout = true;

	Map<Plugin,Edge> edges = new HashMap<Plugin,Edge>();
	String[] names;
	String name;
	int x, y, forceX, forceY;
	double velocity;
	int index;
	int instrument, note;
	boolean beingInspected;
	boolean focus = false;
	double age;
	Font font = new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE);
	Font smallFont = new Font("Courier New", Font.PLAIN, Constants.FONT_SIZE_SMALL);

	
	public Plugin(String name, int index) {
		this.name = name;
		this.index = index;
		names = name
				.replace("com.", "")
				.replace("internal.", "")
				.replace("org.", "")
				.replace("eclipse.", "")
				.split("\\.");
		instrument = UI.currentSoundTheme.getInstrument(name);
		setStartLocation();
		chooseNote();
		setFocus();
	}
	
	void setStartLocation() {
		if (useGraphLayout) {
			forceX = x = Constants.CENTER_X + (int)Math.random() * Constants.PLUGIN_SIZE;
			forceY = y = Constants.CENTER_Y + (int)Math.random() * Constants.PLUGIN_SIZE;
		} else {
			int count = plugins.size();
			int column = count % Constants.PLUGINS_PER_ROW;
			int row = count / Constants.PLUGINS_PER_ROW;
			x = Constants.MARGIN * 2 + Constants.PLUGIN_SIZE * column;
			y = Constants.MARGIN * 2 + Constants.PLUGIN_SIZE * row;
		}
	}
	
	@Override
	public String toString() {
		return "<Plugin " + name + ">";
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
			Edge.clear();
		}
	}

	public static void updateInstruments() {
		for (Plugin plugin: plugins.values()) {
			plugin.instrument = UI.currentSoundTheme.getInstrument(plugin.name);
		}
	}

	private void chooseNote() {
		note = UI.currentSoundTheme.getNote(name);
	}
	
	static void focusUpdated() {
		for (Plugin plugin : plugins.values()) {
			plugin.setFocus();
		}
	}
	
	void setFocus() {
		focus = false;
		if (UI.pluginFilter == "") return;
		String[] filters = UI.pluginFilter.split(",");
		for (String filter: filters) {
			if (filter.length() > 0 && name.indexOf(filter) != -1) {
				focus = true;
			};
		}
	}

	static void drawAll(Graphics2D g) {
		synchronized (plugins) {
			if (useGraphLayout) {
				drawGraph(g);
			} else {
				drawGrid(g);
			}
			for (Plugin plugin : plugins.values()) {
				plugin.age -= 0.15;
			}
			drawStats(g);
		}
	}
	
	static void drawStats(Graphics2D g) {
		g.setColor(Color.YELLOW);
		g.setComposite(Plugin.opaque);
		g.drawString(UI.statistics, 10, Constants.HEIGHT - 170);
	}

	static void drawGrid(Graphics2D g) {
		for (Plugin plugin : plugins.values()) {
			plugin.draw(g);
		}
		for (Plugin plugin : plugins.values()) {
			plugin.drawCalls(g);
		}
	}
	
	static void drawGraph(Graphics2D g) {
		for (Plugin plugin : plugins.values()) {
			plugin.avoidOverlap();
		}
		for (Plugin plugin : plugins.values()) {
			plugin.drawEdges(g);
		}
		for (Plugin plugin : plugins.values()) {
			plugin.drawCalls(g);
		}
		for (Plugin plugin : plugins.values()) {
			plugin.draw(g);
		}
		for (Plugin plugin : plugins.values()) {
			plugin.syncForce();
		}
		centerPlugins(g);
	}
	
	static void centerPlugins(Graphics2D g) {
		int minX = Constants.WIDTH, maxX = 0, minY = Constants.HEIGHT, maxY = 0;
		for (Plugin plugin : plugins.values()) {
			minX = Math.min(minX, plugin.x);
			maxX = Math.max(maxX, plugin.x);
			minY = Math.min(minY, plugin.y);
			maxY = Math.max(maxY, plugin.y);
		}

		int spaceLeft = minX;
		int spaceRight = Constants.WIDTH - maxX - Constants.PLUGIN_SIZE;
		int spaceTop = minY;
		int spaceBottom = Constants.HEIGHT - maxY - 3 * Constants.PLUGIN_SIZE;
		int dx = Math.abs(spaceLeft - spaceRight) > 30 ? (spaceLeft > spaceRight ? -10 : 10) / 2 : 0;
		int dy = Math.abs(spaceTop - spaceBottom) > 30 ? (spaceTop > spaceBottom) ? -10 : 10 / 2 : 0;

		for (Plugin plugin : plugins.values()) {
			plugin.x += dx;
			plugin.y += dy;
			plugin.forceX += dx;
			plugin.forceY += dy;
		}
	}
	
	void avoidOverlap() {
		for (Plugin other: Plugin.plugins.values()) {
			if (this != other) {
				moveAwayFrom(other);
			}
		}
	}
	
	public static boolean select(int x, int y) {
		selectedPlugin = null;
		for (Plugin plugin : plugins.values()) {
			if (plugin.inside(x, y)) {
				selectedPlugin = plugin;
				UI.instrumentSelector.setSelectedIndex(plugin.instrument + 1);
				return true;
			}
		}
		return false;
	}
	
	static Plugin get(String name) {
		Plugin plugin = plugins.get(name);
		if (plugin == null) {
			plugin = new Plugin(name, plugins.size() + 1);
			synchronized (plugins) {
				plugins.put(name,  plugin);
				Edge.update();
			}
		}
		return plugin;
	}

	public boolean asleep() {
		return age < Constants.LAST_ACTIVE_CUTOFF;
	}

	public void draw(Graphics2D g) {
		if (asleep()) return;
		setTransparancy(g);
		setBorderWidth(g);
		setBackground(g);
		int size = Constants.PLUGIN_SIZE;
		int margin = Constants.MARGIN;
		if (useGraphLayout) {
			g.fillOval(x + margin, y + margin, size - 2*margin, size - 2*margin);
		} else {
			g.fillRect(x + margin, y + margin, size - 2*margin, size - 2*margin);
		}
		setBorderColor(g);
		if (useGraphLayout) {
			g.drawOval(x + margin, y + margin, size - 2*margin, size - 2*margin);
		} else {
			g.drawRect(x + margin, y + margin, size - 2*margin, size - 2*margin);
		}
		setTextColor(g);
		g.setFont(font);
		int yOffset = 1 + (size / 11 - names.length) / 2 * 11;
		for (int n=0; n<names.length; n++) {
			int xOffset = 5 + size / 2 - 8 * names[n].length() / 2;
			g.drawString(names[n], x + xOffset, y + yOffset + (n+1) * 11);
		}
		if (instrument != -1) {
			setNumberColor(g);
			g.drawString(String.format("%d", instrument), x + size, y + size - margin - 3);
		}
		if (beingInspected) {
			g.setColor(Color.YELLOW);
			g.setStroke(new BasicStroke(2));
			g.fillOval(x + 15, y + size - margin - 13, 10, 10);
		}
	}
	
	public void setTransparancy(Graphics2D g) {
		Composite transparancy = opaque;
		if (!focus) {
			transparancy = transparancies[Math.max(1, Math.min(transparancies.length - 1, 1 + (int)age))];
		}
		g.setComposite(transparancy);
	}
	
	void setBackground(Graphics2D g) {
		g.setColor(focus ? Color.ORANGE : Color.DARK_GRAY);
	}
	
	void setBorderWidth(Graphics2D g) {
		g.setStroke(new BasicStroke(selectedPlugin == this || focus ? 5 : age < 2 ? 1 : 2));
	}
	
	void setBorderColor(Graphics2D g) {
		g.setColor(Color.GREEN);
	}
	
	private void setTextColor(Graphics2D g) {
		g.setColor(focus ? Color.BLACK : Color.ORANGE);
	}

	private void setNumberColor(Graphics2D g) {
		g.setColor(Color.WHITE);
	}

	public void drawEdges(Graphics2D g) {
		g.setStroke(new BasicStroke(1));
	    for (Edge edge : edges.values()) {
	    	if (edge.weight == 0) {
				edge.drawConnection(g);
	    	}
		}
	}

	public void drawCalls(Graphics2D g) {
	    for (Edge edge : edges.values()) {
			if (edge.weight != 0) {
				edge.draw(g);
			}
		}
	}
	
	public void highlight(Graphics2D g) {
		g.setStroke(new BasicStroke(2));
		draw(g);
	}
	
	public void drawLineTo(Plugin other, int weight, Graphics2D g) {
		drawLineTo(other, weight, Color.WHITE, g);
	}
	
	public void drawLineTo(Plugin other, int weight, Color color, Graphics2D g) {
		g.setColor(color);
		int xOffset = Constants.PLUGIN_SIZE / 2;
		int yOffset = Constants.PLUGIN_SIZE - 20;
		if (weight > 1) {
			g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setComposite(transparancies[weight]);
		} else {
			g.setStroke(new BasicStroke(1));
		}
		g.drawLine(x + xOffset, y + yOffset, other.x + xOffset, other.y + yOffset);
		g.setComposite(opaque);

		if (weight == Constants.HEART_BEAT || UI.overrideBeep) {
			this.beep();
			other.beep();
		}
	}

	private void beep() {
		if (instrument == -1) return;
		UI.orchestra.setInstrument(instrument);
		UI.orchestra.noteOn(note);
		new Thread(new Runnable() {
			public void run() {	
				try { Thread.sleep(Constants.MUTE_DELAY); } catch (Exception e) { }
				UI.orchestra.setInstrument(instrument);
				UI.orchestra.noteOff(note);
			}
		}).start();
	}

	public void toggleInspect() {
		setInspect(!beingInspected);
	}

	public void setInspect(boolean inspect) {
		if (beingInspected == inspect) return;
		beingInspected = inspect;
		UI.sendEvent(beingInspected ? Constants.INSPECT_PLUGIN : Constants.UN_INSPECT_PLUGIN, name);
	}

	public static void called(String fromName, String toName) {
		Plugin from = get(fromName);
		Plugin to = get(toName);
		Edge edge = from.edges.get(to);
		if (edge == null) {
			edge = new Edge(from, to);
		}
		edge.called();
		from.edges.put(to, edge);
		from.age = Math.min(from.age + 1, Constants.JUST_CALLED);
		to.age = Math.min(from.age + 1, Constants.JUST_CALLED);
	}

	void applyForce(Plugin other, int force) {
		forceX += forceX < other.forceX ? force : -force;
		forceY += forceY < other.forceY ? force : -force;
	}

	void syncForce() {
		x += Math.round((forceX - x) * velocity);
		y += Math.round((forceY - y) * velocity);
		if (y > 1000) System.out.println("Move off screen: " +this + " " + forceX + " " + x + " " + forceY + " " + y);
		velocity /= 2;
		keepOnScreen();
	}

	void keepOnScreen() {
		x = Math.max(0, Math.min(Constants.WIDTH - Constants.PLUGIN_SIZE, x));
		y = Math.max(0, Math.min(Constants.HEIGHT - 3 * Constants.PLUGIN_SIZE, y));
	}
	
	int getDistance(Plugin other) {
		return (int)Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
	}

	private boolean inside(int ex, int ey) {
		return ex > x && ey > y && ex < x + Constants.PLUGIN_SIZE && ey < y + Constants.PLUGIN_SIZE;
	}

	public boolean overlaps(Plugin other, int margin) {
		if (forceX >= other.forceX + Constants.PLUGIN_SIZE + margin) return false;
		if (forceY >= other.forceY + Constants.PLUGIN_SIZE + margin) return false;
		if (other.forceX >= forceX + Constants.PLUGIN_SIZE + margin) return false;
		if (other.forceY >= forceY + Constants.PLUGIN_SIZE + margin) return false;
		return true;
	}

	public void moveAwayFrom(Plugin other) {
		if (asleep() || other.asleep()) return;
		int margin = 3 * Constants.MARGIN;
		if (overlaps(other, margin * 3)) {
			int size = Constants.PLUGIN_SIZE;
			int extra = (int) (Math.random() * 3);
			if (Math.random() >= 0.5)
				forceX = (x >= other.x ? other.x + size + margin : x - size - margin) + extra;
			else
				forceY = (y >= other.y ? other.y + size + margin : y - size - margin) + extra;
			velocity = Constants.FORCE_VELOCITY;
		}
	}

	public static void move(int x, int y) {
		if (selectedPlugin != null) {
			selectedPlugin.x = selectedPlugin.forceX = x;
			selectedPlugin.y = selectedPlugin.forceY = y;
		}
	}

	public static void hover(int x, int y) {
		for (Plugin plugin : plugins.values()) {
			if (!plugin.asleep() && plugin.inside(x, y)) {
				plugin.age = Constants.JUST_CALLED;
			}
		}
	}
}

class Edge {
	static int idealLength;
	static int maxCalls;
	static Plugin center = new Plugin("center", -1);
	int weight = Constants.HEART_BEAT;
	int totalCalls;
	Plugin from;
	Plugin to;

	public Edge(Plugin from, Plugin to) {
		this.from = from;
		this.to = to;
	}	

	public void called() {
		weight = Constants.HEART_BEAT;
		from.velocity = Constants.FORCE_VELOCITY;
		maxCalls = Math.max(totalCalls++, maxCalls);
	}
	
	public static void clear() {
		maxCalls = 0;
		update();
	}
	
	public void drawConnection(Graphics2D g) {
		if (from.age < Constants.LAST_ACTIVE_CUTOFF || to.age < Constants.LAST_ACTIVE_CUTOFF) return;
		int transparancyBlockSize = 1 + maxCalls / Plugin.transparancies.length;
		int index = Math.min(1 + totalCalls / transparancyBlockSize, Plugin.transparancies.length - 1);
		g.setComposite(Plugin.transparancies[(int)Math.max(1, Math.min(from.age, to.age))]);
		g.setStroke(new BasicStroke(index * 2));
		from.drawLineTo(to, index, Color.PINK, g);
		g.setComposite(Plugin.opaque);
	}
	
	public void draw(Graphics2D g) {
		g.setStroke(new BasicStroke(Math.max(1,  weight)));
		from.drawLineTo(to, weight, Color.WHITE, g);
		weight--;
	}

	static void update() {
		idealLength = (int)(Constants.WIDTH / Math.sqrt(Plugin.plugins.size() + 1));
		idealLength = Math.min(150, idealLength);
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Edge)) return false;
		Edge edge = (Edge)object;
		return to.equals(edge.to);
	}

	public int hashCode() {
		return to.hashCode();
	}
	
}