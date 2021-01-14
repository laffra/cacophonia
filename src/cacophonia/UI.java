package cacophonia;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


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
	static Orchestra orchestra = new Orchestra();

	static final int WIDTH = 1200;
	static final int HEIGHT = 1200;
	static final int PLUGINS_PER_ROW = 13;
	static final int MARGIN = 5;
	static final int PLUGIN_SIZE = 90;
	static final int REDRAW_DELAY = 150; 
	static final int HISTORY_SECONDS = 60; 
	static final int HISTORY_SAMPLES_PER_SECOND = 1000 / REDRAW_DELAY; 
	static final int HISTORY_SIZE = HISTORY_SECONDS * HISTORY_SAMPLES_PER_SECOND; 
	static final int HISTORY_SLIDER_SIZE = HISTORY_SIZE / 2;
	static final int HISTORY_INCREMENT = 2;
	static final int MUTE_DELAY = 1500;
	static final int HEART_BEAT = 3;
	
	static Image drawing = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
	static HashMap<String, Boolean> called = new HashMap<>();
	static HashMap<String, Integer> scores = new HashMap<>();
	static List<HashMap<String, Integer>> history = new ArrayList<HashMap<String, Integer>>();
	static int instrumentStartNumber;
	static JComboBox<Object> instrumentSelector;
	static JLabel time;
	static DataOutputStream eventStream;
	static String pluginFilter = "";
	static boolean live = true;
	static int historyIndex;
	static boolean muted = true;
	static boolean manual = false;
	static Date historyFrozen = new Date();
	static boolean overrideBeep = false;
	static TimeTravelerBar timeTravelerBar;
	

	public static void main(String args[]) {
		try {
			setupListener();
	    	createUI();
			initHistory();
		    updateDisplay();
			setupRedrawLoop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void setupRedrawLoop() {
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(REDRAW_DELAY);
						synchronized (called) {
							if (live) updateScores();
							updateDisplay();
							if (live) updateHistory();
							overrideBeep = false;
						}
					} catch (Exception e) {
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
						} catch (Exception e) {
							serverSocket.close();
							System.out.println("Exit UI");
							System.out.flush();
							e.printStackTrace();
							System.err.flush();
							System.exit(0);
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
					System.err.flush();
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
	
	private static void updateScores() {
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
	
	private static void updateDisplay() {
		drawing = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = (Graphics2D) drawing.getGraphics();
	    Plugin.drawAll((Graphics2D) g);
	    g.setColor(Color.WHITE);
	    updateDrawing(g, false);
	    updateDrawing(g, true);
	    g.dispose();
    	canvas.repaint();
    	timeTravelerBar.repaint();
	}
	
	private static void initHistory() {
		for (int n=0; n<HISTORY_SIZE; n++) {
			history.add(new HashMap<String,Integer>());
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void updateHistory() {
	    scores.entrySet().removeIf(entry -> entry.getValue() == 0);
	    history.remove(0);
	    history.add((HashMap<String,Integer>)scores.clone());
	    updateTime();
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
		    	if (live) {
			    	value = Math.max(value - 1, 0);
			    	scores.put(key, value);
		    	}
	    	} else {
	    		from.highlight(g);
	    		to.highlight(g);
	    	}
	    }
	}

	private static void createUI() {
		JFrame frame = new JFrame("Eclipse Cacophonia");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container mainContainer = frame.getContentPane();
		mainContainer.setLayout(new BorderLayout());
		
		Container header = createHeader();
		header.add(createThemeUI());
		header.add(new Label("|"));
		header.add(createManualUI());
		header.add(new Label("|"));
		header.add(createFilterUI());
		header.add(new Label("|"));
		header.add(createHistoryUI());
		mainContainer.add(header, BorderLayout.NORTH);

		canvas = new CacophoniaCanvas();
		mainContainer.add(canvas, BorderLayout.CENTER);
		
		frame.setLocation(2300, 5);
		frame.setSize(WIDTH, HEIGHT);
		frame.setVisible(true);
	}

	private static Component createFilterUI() {
		Container container = new Container();
		container.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		Button clear = new Button("clear");
		clear.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				Plugin.clear();
			}
		});
		container.add(clear);
		container.add(new Label("filter:"));
		TextField filter = new TextField(16);
		filter.addTextListener(new TextListener() {	
			@Override
			public void textValueChanged(TextEvent e) {
				UI.pluginFilter = filter.getText();
			}
		});
		container.add(filter);
		return container;
	}

	private static Container createHeader() {
		Container header = new Container();
		header.setPreferredSize(new Dimension(800, 40));
		header.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
		header.setPreferredSize(new Dimension(WIDTH,40));
		return header;
	}

	private static Component createThemeUI() {
		Container container = new Container();
		container.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		JCheckBox checkbox = new JCheckBox("mute", true);
		checkbox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				UI.muted = checkbox.isSelected();
				if (UI.muted) {
					UI.orchestra.allNotesOff();
				}
			}
		});
		container.add(checkbox);
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
		container.add(themeChooser);
		return container;
	}

	private static Component createManualUI() {
		Checkbox manual = new Checkbox("manual", false);
		manual.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				UI.manual = !manual.getState();
				Plugin.clearInstruments();
			}
		});
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
		Container container = new Container();
		container.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		container.add(manual);
		container.add(previous);
		container.add(instrumentSelector);
		container.add(next);
		return container;
	}
	
	private static void updateTime() {
		Date date = new Date();
		if (!live) {
			int secondsBack = HISTORY_SECONDS - historyIndex * HISTORY_SECONDS / HISTORY_SIZE;
			Calendar calendar=Calendar.getInstance();
			calendar.setTime(historyFrozen);
			calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - secondsBack);
			date = calendar.getTime();
		}
		time.setText(new SimpleDateFormat("HH:mm:ss").format(date));
	}
	
	private static void windBackTime(JCheckBox live, int index) {
		timeTravelerBar.setValue(index);
		historyIndex = timeTravelerBar.getValue();
		if (UI.live) historyFrozen = new Date();
		live.setSelected(false);
		UI.live = false;
		updateTime();
		HashMap<String,Integer>historyScores = new HashMap<String,Integer>();
		for (int n=0; n<HISTORY_SAMPLES_PER_SECOND; n++) {
			if (historyIndex + n < HISTORY_SIZE) {
				HashMap<String,Integer> sample = (HashMap<String,Integer>)history.get(historyIndex + n);
				historyScores.putAll(sample);
			}
		}
		scores = historyScores;
		overrideBeep = true;
	}

	private static Component createHistoryUI() {		
		JCheckBox live = new JCheckBox("live", true);
		timeTravelerBar = new TimeTravelerBar();
		time = new JLabel();
		time.setPreferredSize(new Dimension(65, 30));
		live.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				timeTravelerBar.setValue(HISTORY_SIZE);
				UI.live = live.isSelected();
			}
		});
		timeTravelerBar.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				windBackTime(live, timeTravelerBar.getValue());
 			}
		});
		Container container = new Container();
		container.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		container.add(time);
		container.add(timeTravelerBar);
		container.add(live);		
		return container;
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

		if (weight == UI.HEART_BEAT || UI.overrideBeep) {
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
				try { Thread.sleep(UI.MUTE_DELAY); } catch (Exception e) { }
				UI.orchestra.setInstrument(instrument);
				UI.orchestra.noteOff(note);
			}
		}).start();
	}
}


/**
 * A component that shows the plugins active in the past and allows the user to select a point in time to explore.
 */
class TimeTravelerBar extends JPanel {
	private static final int TIME_TRAVELER_WIDTH = 120;
	private static final int TIME_TRAVELER_HEIGHT = 30;
	private int value;
	private ChangeListener listener;
	
	public TimeTravelerBar() {
		setBackground (Color.BLACK);  
	    setPreferredSize(new Dimension(TIME_TRAVELER_WIDTH, TIME_TRAVELER_HEIGHT));  
	    addMouseMotionListener(new MouseAdapter() {
	    	public void mouseMoved(MouseEvent e) {
	    		value = e.getX() * UI.HISTORY_SIZE / TIME_TRAVELER_WIDTH;
	    		repaint();
    			listener.stateChanged(null);
	    	}
	    });
	}
    public void addChangeListener(ChangeListener listener) {
		this.listener = listener;
	}
	public void setValue(int value) {
		this.value = value;
	}
	public int getValue() {
		return value;
	}
	public void paintComponent(Graphics graphics) { 
		Graphics2D g = (Graphics2D)graphics;
    	int index = 0;
    	g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Color.RED);
        for (HashMap<String, Integer> scores: UI.history) {
        	if (scores.size() > 0) {
        		int x = TIME_TRAVELER_WIDTH * index / UI.HISTORY_SIZE;
        		int h = 5 * (int)Math.log(scores.size());
            	g.drawLine(x, TIME_TRAVELER_HEIGHT - h, x, TIME_TRAVELER_HEIGHT);
        	}
        	index += 1;
        }
    	g.setColor(Color.YELLOW);
    	g.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    	int x = TIME_TRAVELER_WIDTH * value / UI.HISTORY_SIZE;
    	g.drawLine(x, 0, x, TIME_TRAVELER_HEIGHT);
	} 
}



/**
 * The canvas used to draw plugins on. Mouse clicks enable/disable sounds.
 */
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