package cacophonia.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import java.util.prefs.Preferences;

import javax.sound.midi.Instrument;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cacophonia.Constants;


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
	static Image drawing = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_ARGB);
	static HashMap<String, Boolean> called = new HashMap<>();
	static HashMap<String, Integer> scores = new HashMap<>();
	static List<HashMap<String, Integer>> history;
	static JComboBox<Object> instrumentSelector;
	static JLabel time;
	static DataOutputStream eventStream;
	static String pluginFilter = "";
	static boolean live = true;
	static int historyIndex;
	static boolean muted = true;
	static Date historyFrozen = new Date();
	static boolean overrideBeep = false;
	static FluxController fluxController;
	static SoundTheme currentSoundTheme;
	static Preferences preferences = Preferences.userNodeForPackage(UI.class);
	

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
						Thread.sleep(Constants.REDRAW_DELAY);
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
		    	scores.put(key, Constants.HEART_BEAT);
	    	}
	    	called.put(key, false);
	    }
	}
	
	private static void updateDisplay() {
		drawing = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = (Graphics2D) drawing.getGraphics();
	    Plugin.drawAll((Graphics2D) g);
	    g.setColor(Color.WHITE);
	    updateDrawing(g, false);
	    updateDrawing(g, true);
	    g.dispose();
    	canvas.repaint();
    	fluxController.repaint();
	}
	
	private static void initHistory() {
		history = new ArrayList<HashMap<String, Integer>>();
		for (int n=0; n<Constants.HISTORY_SIZE; n++) {
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
		header.add(createInstrumentSelector());
		header.add(new Label("|"));
		header.add(createFilterUI());
		header.add(new Label("|"));
		header.add(createHistoryUI());
		mainContainer.add(header, BorderLayout.NORTH);

		canvas = new CacophoniaCanvas();
		mainContainer.add(canvas, BorderLayout.CENTER);
		
		frame.setLocation(2300, 5);
		frame.setSize(Constants.WIDTH, Constants.HEIGHT);
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
				UI.initHistory();
			}
		});
		container.add(clear);
		container.add(new Label("filter:"));
		TextField filter = new TextField(10);
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
		header.setPreferredSize(new Dimension(Constants.WIDTH,40));
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
		for (SoundTheme theme: SoundTheme.themes) {
			themeChooser.add(theme.name);  
		}
		themeChooser.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				selectSoundTheme(themeChooser.getSelectedIndex());
				UI.preferences.put("lastTheme", ""+themeChooser.getSelectedIndex());
			}
		});
		int lastTheme = Integer.parseInt(UI.preferences.get("lastTheme", "0"));
		themeChooser.select(lastTheme);
		selectSoundTheme(lastTheme);
		container.add(themeChooser);
		return container;
	}
	
	static void selectSoundTheme(int index) {
		UI.currentSoundTheme = SoundTheme.themes[index];
		Plugin.updateInstruments();
		System.out.println("Theme changed to " + UI.currentSoundTheme.name);
	}
	
	private static Component createInstrumentSelector() {
		List<String> instrumentNames = new ArrayList<String>();
		instrumentNames.add("None");
		Instrument instruments[] = UI.orchestra.synthesizer.getAvailableInstruments();
		for (int n=0; n<instruments.length; n++) {
       		instrumentNames.add(String.format("%d - %s", n, instruments[n].getName()));
		}
		instrumentSelector = new JComboBox<Object>(instrumentNames.toArray());
		instrumentSelector.setMaximumRowCount(64);
		instrumentSelector.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				if (Plugin.selectedPlugin != null) {
					Plugin.selectedPlugin.setInstrument(instrumentSelector.getSelectedIndex() - 1);
				}
			}
		});
		Button previous = new Button("<");
		previous.setPreferredSize(new Dimension(25, 20));
		previous.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (Plugin.selectedPlugin != null) {
					Plugin plugin = Plugin.selectedPlugin;
					plugin.setInstrument(Math.max(plugin.instrument - 1, -1));
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
					plugin.setInstrument(Math.max(plugin.instrument + 1, -1));
					instrumentSelector.setSelectedIndex(plugin.instrument + 1);
				}
			}
		});
		Container container = new Container();
		container.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		container.add(previous);
		container.add(instrumentSelector);
		container.add(next);
		return container;
	}
	
	private static void updateTime() {
		Date date = new Date();
		if (!live) {
			int secondsBack = Constants.HISTORY_SECONDS - historyIndex * Constants.HISTORY_SECONDS / Constants.HISTORY_SIZE;
			Calendar calendar=Calendar.getInstance();
			calendar.setTime(historyFrozen);
			calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) - secondsBack);
			date = calendar.getTime();
		}
		time.setText(new SimpleDateFormat("HH:mm:ss").format(date));
	}
	
	private static void travelBackInTime(JCheckBox live, int index) {
		fluxController.setValue(index);
		historyIndex = fluxController.getValue();
		if (UI.live) historyFrozen = new Date();
		live.setSelected(false);
		UI.live = false;
		updateTime();
		HashMap<String,Integer>historyScores = new HashMap<String,Integer>();
		for (int n=0; n<Constants.HISTORY_SAMPLES_PER_SECOND; n++) {
			if (historyIndex + n < Constants.HISTORY_SIZE) {
				HashMap<String,Integer> sample = (HashMap<String,Integer>)history.get(historyIndex + n);
				historyScores.putAll(sample);
			}
		}
		scores = historyScores;
		overrideBeep = true;
	}

	private static Component createHistoryUI() {		
		JCheckBox live = new JCheckBox("live", true);
		fluxController = new FluxController();
		time = new JLabel();
		time.setPreferredSize(new Dimension(65, 30));
		Button previous = new Button("<");
		previous.setPreferredSize(new Dimension(25, 20));
		previous.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				travelBackInTime(live, fluxController.getValue() - 1);
			}
		});
		Button next = new Button(">");
		next.setPreferredSize(new Dimension(25, 20));
		next.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				travelBackInTime(live, fluxController.getValue() + 2);
			}
		});
		live.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				UI.live = live.isSelected();
				if (UI.live) {
					fluxController.setValue(Constants.HISTORY_SIZE);
				}
			}
		});
		fluxController.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				travelBackInTime(live, fluxController.getValue());
 			}
		});
		Container container = new Container();
		container.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 1));
		container.add(time);
		container.add(previous);
		container.add(fluxController);
		container.add(next);
		container.add(live);		
		return container;
	}  
}