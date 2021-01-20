package cacophonia.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.util.HashMap;

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
	String[] names;
	String name;
	int x, y, index;
	int instrument, note;
	boolean beingInspected;
	
	public Plugin(String name, int index) {
		this.name = name;
		this.index = index;
		names = name
				.replace("com.", "")
				.replace("internal.", "")
				.replace("org.", "")
				.replace("eclipse.", "")
				.split("\\.");
		int count = plugins.size();
		int column = count % Constants.PLUGINS_PER_ROW;
		int row = count / Constants.PLUGINS_PER_ROW;
		x = Constants.MARGIN * 2 + Constants.PLUGIN_SIZE * column;
		y = Constants.MARGIN * 2 + Constants.PLUGIN_SIZE * row;
		instrument = UI.currentSoundTheme.getInstrument(name);
		chooseNote();
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
		for (Plugin plugin: plugins.values()) {
			plugin.setInspect(false);
		}
		plugins.clear();
		selectedPlugin = null;
	}

	public static void updateInstruments() {
		for (Plugin plugin: plugins.values()) {
			plugin.instrument = UI.currentSoundTheme.getInstrument(plugin.name);
		}
	}

	private void chooseNote() {
		note = UI.currentSoundTheme.getNote(name);
	}
	
	static void drawAll(Graphics2D g) {
		for (Plugin plugin : plugins.values()) {
			if (UI.pluginFilter != "" && plugin.name.indexOf(UI.pluginFilter) == -1)
				continue;
			plugin.draw(g);
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
	
	private boolean inside(int ex, int ey) {
		return ex > x && ey > y && ex < x + Constants.PLUGIN_SIZE && ey < y + Constants.PLUGIN_SIZE;
	}

	static Plugin get(String name) {
		Plugin plugin = plugins.get(name);
		if (plugin == null) {
			plugin = new Plugin(name, plugins.size() + 1);
			plugins.put(name,  plugin);
		}
		return plugin;
	}
	
	public void draw(Graphics2D g) {
		draw(g, Color.GREEN, Color.ORANGE, Color.WHITE);
	}
	
	public void draw(Graphics2D g, Color borderColor, Color textColor, Color numberColor) {
		g.setStroke(new BasicStroke(selectedPlugin == this ? 5 : 1));
		g.setColor(Color.DARK_GRAY);
		g.fillRect(x + Constants.MARGIN, y + Constants.MARGIN, Constants.PLUGIN_SIZE - 2*Constants.MARGIN, Constants.PLUGIN_SIZE - 2*Constants.MARGIN);
		g.setColor(borderColor);
		g.drawRect(x + Constants.MARGIN, y + Constants.MARGIN, Constants.PLUGIN_SIZE - 2*Constants.MARGIN, Constants.PLUGIN_SIZE - 2*Constants.MARGIN);
		g.setColor(textColor);
		for (int n=0; n<names.length; n++) {
			g.drawString(names[n], x + 15, y + Constants.MARGIN + (n+1) * 15);
		}
		if (instrument != -1) {
			g.setColor(numberColor);
			g.drawString(String.format("%d", instrument), x + Constants.PLUGIN_SIZE/2, y + Constants.PLUGIN_SIZE - Constants.MARGIN - 3);
		}
		if (beingInspected) {
			g.setColor(Color.YELLOW);
			g.setStroke(new BasicStroke(2));
			g.fillOval(x + 15, y + Constants.PLUGIN_SIZE - Constants.MARGIN - 13, 10, 10);
		}
	}
	
	public void highlight(Graphics2D g) {
		g.setStroke(new BasicStroke(2));
		draw(g, Color.RED, Color.ORANGE, Color.WHITE);
	}
	
	public void drawLineTo(Plugin other, int weight, Graphics2D g) {
		g.setColor(Color.WHITE);
		int offset = Constants.PLUGIN_SIZE/2;
		g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setComposite(transparancies[weight]);
		g.drawLine(x + offset, y + offset, other.x + offset, other.y + offset);
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
}