package cacophonia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.JComboBox;
import javax.swing.JFrame;


/**
 * Receives events from an Eclipse instance to visualize a call made between two plugins.
 * 
 * The UI consists of a grid of rectangles where each represents one plugin. When a call is made between two plugins the following happens:
 * <ul>
 * <li> A line is drawn between the two plugins in a contrasting color. The line slowly fades out.
 * <li> A sound is played to indicate which plugins are involved. Each plugin uses a different instrument and tone.
 * </ul>
 */
public class UI {
	static CacophoniaCanvas canvas;
	static int WIDTH = 1200;
	static int HEIGHT = 1200;
	static int PLUGINS_PER_ROW = 13;
	static int MARGIN = 5;
	static int PLUGIN_SIZE = 90;
	static int REDRAW_DELAY = 150; 
	static int MUTE_DELAY = 1500;
	static int HEART_BEAT = 3;
	static boolean muted = true;
	static boolean manual = false;
	static Orchestra orchestra = new Orchestra();
	
	static Image drawing = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
	static HashMap<String, Boolean> called = new HashMap<>();
	static HashMap<String, Integer> scores = new HashMap<>();
	protected static int instrumentStartNumber;
	static JComboBox<Object> instrumentSelector;
	static DataOutputStream eventStream;

	public static void main(String args[]) {
		setupListener();
    	createUI();
	    updateDisplay();
		setupRedrawLoop();
	}
	
	private static void setupRedrawLoop() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(REDRAW_DELAY);
						synchronized (called) {
							updateScores();
							updateDisplay();
						}
					} catch (Exception e) {
						System.err.println(e);
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	private static void setupListener() {
		new Thread(new Runnable() {
			public void run() {
				try{  
					ServerSocket serverSocket = new ServerSocket(6666);  
					Socket socket = serverSocket.accept();   
					DataInputStream dis=new DataInputStream(socket.getInputStream());  
					eventStream = new DataOutputStream(socket.getOutputStream());
					while (true) {
						try {
							String pluginToPlugin = (String)dis.readUTF();
							synchronized (called) {
								if (pluginToPlugin.indexOf(" ") != -1) {
									called.put(pluginToPlugin, true);
								}
							}
						} catch(EOFException e) {
							serverSocket.close();
							System.exit(0);
						} catch (Exception e) {
							System.err.println(e);
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
				}  
			}
		}).start();
	}

	static void sendEvent(int command, String details) {
		try {
			eventStream.writeInt(command);
			eventStream.writeUTF(details);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static void updateScores() {
	    for (Map.Entry<String,Boolean> entry : called.entrySet()) {
	    	String key = entry.getKey();
	    	boolean value = entry.getValue();
	    	if (value) {
	    		// A call was made between the two plug-ins in the last 200ms
		    	scores.put(key, HEART_BEAT);
	    	}
	    	called.put(key, false);
	    }
	}
	
	protected static void updateDisplay() {
		drawing = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = (Graphics2D) drawing.getGraphics();
	    Plugin.drawAll((Graphics2D) g);
	    g.setColor(Color.WHITE);
	    updateDrawing(g, false);
	    updateDrawing(g, true);
	    g.dispose();
    	canvas.repaint();
	}
	
	static void updateDrawing(Graphics2D g, boolean drawLine) {
	    for (Map.Entry<String,Integer> entry : scores.entrySet()) {
	    	String key = entry.getKey();
	    	int value = entry.getValue();
	    	if (value == 0) continue;
	    	String pluginNames[] = key.split(" ");
	    	Plugin from = Plugin.get(pluginNames[0]);
	    	Plugin to = Plugin.get(pluginNames[1]);
	    	if (drawLine) {
		    	from.drawLineTo(to, value, g);
		    	scores.put(key, Math.max(value - 1, 0));
	    	} else {
	    		from.highlight(g);
	    		to.highlight(g);
	    	}
	    }
	}

	private static void createUI() {
		JFrame frame = new JFrame("Eclipse Cacophonia");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container container = frame.getContentPane();
		container.setLayout(new BorderLayout());
		
		Container header = new Container();
		header.setPreferredSize(new Dimension(800, 40));
		header.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
		header.setPreferredSize(new Dimension(WIDTH,40));
		Checkbox checkbox = new Checkbox("mute", true);
		checkbox.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				UI.muted = !checkbox.getState();
				if (UI.muted) {
					UI.orchestra.allNotesOff();
				}
			}
		});
		header.add(checkbox);
		Choice themeChooser = new Choice();
		for (SoundTheme theme: SoundTheme.getAllThemes()) {
			themeChooser.add(theme.name);  
		}
		themeChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				SoundTheme theme = SoundTheme.getAllThemes()[themeChooser.getSelectedIndex()];
				UI.instrumentStartNumber = theme.startNumber;
				Plugin.updateInstruments();
			}
		});
		header.add(themeChooser);
		header.add(new Label("|"));
		Button clear = new Button("clear");
		clear.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				Plugin.clear();
			}
		});
		header.add(clear);
		header.add(new Label("|"));
		Checkbox manual = new Checkbox("manual", false);
		manual.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				UI.manual = !manual.getState();
				Plugin.clearInstruments();
			}
		});
		header.add(manual);
		List<String> instrumentNames = new ArrayList<String>();
		instrumentNames.add("None");
		for (int n=0; n<SoundTheme.instruments.length; n++) {
       		instrumentNames.add(String.format("%d - %s", n, SoundTheme.instruments[n].getName()));
		}
		instrumentSelector = new JComboBox<Object>(instrumentNames.toArray());
		instrumentSelector.setMaximumRowCount(64);
		instrumentSelector.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				if (Plugin.selectedPlugin != null) {
					Plugin.selectedPlugin.instrument = instrumentSelector.getSelectedIndex() - 1;
				}
			}
		});
		Button previous = new Button("<");
		previous.setPreferredSize(new Dimension(25, 20));
		previous.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Plugin.selectedPlugin != null) {
					Plugin plugin = Plugin.selectedPlugin;
					plugin.instrument = Math.max(plugin.instrument - 1, -1);
					instrumentSelector.setSelectedIndex(plugin.instrument + 1);
				}
			}
		});
		Button next = new Button(">");
		next.setPreferredSize(new Dimension(25, 20));
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Plugin.selectedPlugin != null) {
					Plugin plugin = Plugin.selectedPlugin;
					plugin.instrument = Math.max(plugin.instrument + 1, -1);
					instrumentSelector.setSelectedIndex(plugin.instrument + 1);
				}
			}
		});
		header.add(previous);
		header.add(instrumentSelector);
		header.add(next);
		container.add(header, BorderLayout.NORTH);
		
		canvas = new CacophoniaCanvas();
		container.add(canvas, BorderLayout.CENTER);
		
		frame.setLocation(2300, 5);
		frame.setSize(WIDTH, HEIGHT);
		frame.setVisible(true);
	}  
}


class SoundTheme {
	int startNumber;
	String name;
	static Instrument instruments[];
	
	public SoundTheme(String name, int startNumber) {
		this.name = name;
		this.startNumber = startNumber;
	}
	
	static SoundTheme[] getAllThemes() {
		return new SoundTheme[] {
				new SoundTheme("Suspense", 120),
				new SoundTheme("Ping", 146),
				new SoundTheme("Computer", 94),
				new SoundTheme("Transmission", 114),
				new SoundTheme("Spring", 7),
				new SoundTheme("Summer", 135),
				new SoundTheme("Fall", 137),
				new SoundTheme("Gamelang", 141),
		};
	}
	
	static {
		instruments = UI.orchestra.synthesizer.getAvailableInstruments();
	}
}


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
		int column = count % UI.PLUGINS_PER_ROW;
		int row = count / UI.PLUGINS_PER_ROW;
		x = UI.MARGIN * 2 + UI.PLUGIN_SIZE * column;
		y = UI.MARGIN * 2 + UI.PLUGIN_SIZE * row;
		instrument = UI.orchestra.getNextInstrument(name);
		chooseNote();
	}

	public static void clearInstruments() {
		for (Plugin plugin: plugins.values()) {
			plugin.instrument = -1;
		}
	}
	
	public static void clear() {		
		plugins.clear();
	}

	public static void updateInstruments() {
		for (Plugin plugin: plugins.values()) {
			plugin.instrument = UI.orchestra.getNextInstrument(plugin.name);
		}
	}

	private void chooseNote() {
		note = (int)(Math.random() * 128);		// A totally random note
		note = (50 + plugins.size()) % 128;  	// Make the tone depend on how recent the plugin was loaded
		note = 50 + name.hashCode() % 78;		// A note related to the plugin's name, random, but the same each run
	}
	
	static void drawAll(Graphics2D g) {
		for (Plugin plugin : plugins.values()) {
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
		return ex > x + UI.MARGIN && ey > y + UI.MARGIN && ex < x + UI.PLUGIN_SIZE - UI.MARGIN && ey < y + UI.PLUGIN_SIZE - UI.MARGIN;
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
		g.fillRect(x + UI.MARGIN, y + UI.MARGIN, UI.PLUGIN_SIZE - 2*UI.MARGIN, UI.PLUGIN_SIZE - 2*UI.MARGIN);
		g.setColor(borderColor);
		g.drawRect(x + UI.MARGIN, y + UI.MARGIN, UI.PLUGIN_SIZE - 2*UI.MARGIN, UI.PLUGIN_SIZE - 2*UI.MARGIN);
		g.setColor(textColor);
		for (int n=0; n<names.length; n++) {
			g.drawString(names[n], x + 15, y + UI.MARGIN + (n+1) * 15);
		}
		if (instrument != -1) {
			g.setColor(numberColor);
			g.drawString(String.format("%d", instrument), x + UI.PLUGIN_SIZE/2, y + UI.PLUGIN_SIZE - UI.MARGIN - 3);
		}
		if (beingInspected) {
			g.setColor(Color.YELLOW);
			g.setStroke(new BasicStroke(2));
			g.fillOval(x + 15, y + UI.PLUGIN_SIZE - UI.MARGIN - 13, 10, 10);
		}
	}
	
	public void highlight(Graphics2D g) {
		g.setStroke(new BasicStroke(2));
		draw(g, Color.RED, Color.ORANGE, Color.WHITE);
	}
	
	public void drawLineTo(Plugin other, int weight, Graphics2D g) {
		g.setColor(Color.WHITE);
		int offset = UI.PLUGIN_SIZE/2;
		g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setComposite(transparancies[weight]);
		g.drawLine(x + offset, y + offset, other.x + offset, other.y + offset);
		g.setComposite(opaque);

		if (weight == UI.HEART_BEAT) {
			other.beep();
		}
	}

	private void beep() {
		if (instrument == -1) return;
		UI.orchestra.setInstrument(instrument);
		UI.orchestra.noteOn(note);
		new Thread(new Runnable() {
			public void run() {	
				try { Thread.sleep(UI.MUTE_DELAY); } catch (Exception e) { }
				UI.orchestra.setInstrument(instrument);
				UI.orchestra.noteOff(note);
			}
		}).start();
	}
}


/**
 * The canvas used to draw plugins on. Mouse clicks enable/disable sounds.
 */
@SuppressWarnings("serial")
class CacophoniaCanvas extends Canvas {  
	    public CacophoniaCanvas() { 
	    setBackground (Color.BLACK);  
	    setSize(WIDTH, HEIGHT);  
	    addMouseListener(new MouseAdapter() {
	    	@Override
	    	public void mouseClicked(MouseEvent e) {
	    		if (Plugin.select(e.getX(), e.getY())) {
	    			super.mouseClicked(e);
	    			if (e.getButton() != 1) {
	    				Plugin plugin = Plugin.selectedPlugin;
	    			    PopupMenu menu = new PopupMenu();
    					MenuItem inspect = new MenuItem(plugin.beingInspected ? "Stop inspecting this plugin" : "Inspect this plugin");
	    			    menu.add(inspect);
    					inspect.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								plugin.beingInspected = !plugin.beingInspected;
								UI.sendEvent(plugin.beingInspected ? Constants.INSPECT_PLUGIN : Constants.UN_INSPECT_PLUGIN, plugin.name);
								CacophoniaCanvas.this.remove(menu);
							}
						});
    					MenuItem source = new MenuItem("Import this plugin as source");
	    			    menu.add(source);
	    			    source.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								UI.sendEvent(Constants.IMPORT_PLUGIN_FROM_SOURCE, plugin.name);
								CacophoniaCanvas.this.remove(menu);
							}
						});
    					MenuItem repository = new MenuItem("Import this plugin from repository");
	    			    menu.add(repository);
	    			    repository.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								UI.sendEvent(Constants.IMPORT_PLUGIN_FROM_REPOSITORY, plugin.name);
								CacophoniaCanvas.this.remove(menu);
							}
						});
    					CacophoniaCanvas.this.add(menu);
	    				menu.show(CacophoniaCanvas.this, e.getX(), e.getY());
	    			}
	    		}
	    	}
		});
	}  
    public void paint(Graphics g) { 
        g.drawImage(UI.drawing, 0, 0, this);
	}  
}


/**
 * Creates sounds for plugins.
 */
class Orchestra {
	MidiChannel midiChannel;
	int volume;
	Synthesizer synthesizer;
	    
	public Orchestra() {
		try {
			synthesizer = MidiSystem.getSynthesizer();
			synthesizer.open();
			MidiChannel[] allChannels = synthesizer.getChannels();
			midiChannel = allChannels[0];
		} catch (MidiUnavailableException e) {
			throw new IllegalStateException("Midi support is not available!");
		}
		volume = 80;
	}
	
	public void setVolume(int volumeLevel) {
		volume = volumeLevel;
	}
	
	public void noteOn(int noteNumber) {
		if (!UI.muted) {
			midiChannel.noteOn( noteNumber, volume);
		}
	}
	
	public void noteOff(int noteNumber) {
		midiChannel.noteOff( noteNumber );
	}
	
	public void allNotesOff() {
		midiChannel.allNotesOff();
	}
	
	public int getNextInstrument(String pluginName) {
		if (UI.manual) return -1;
		return Math.abs(UI.instrumentStartNumber + pluginName.hashCode()) % 128;
	}
	
	public void setInstrument(int instrumentNumber) {
		midiChannel.programChange(instrumentNumber);
	}	
}