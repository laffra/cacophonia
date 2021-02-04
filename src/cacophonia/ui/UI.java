package cacophonia.ui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;

import javax.sound.midi.Instrument;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cacophonia.Constants;
import cacophonia.DetailLevel;
import cacophonia.ui.graph.Graph;
import cacophonia.ui.graph.PopupMenuListener;
import cacophonia.ui.graph.Settings;


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
	static Orchestra orchestra = new Orchestra();
	static HashMap<String, Boolean> called = new HashMap<>();
	static HashMap<String, Integer> scores = new HashMap<>();
	static JComboBox<Object> instrumentSelector;
	static JLabel time;
	static DataOutputStream eventStream;
	static String pluginFilter = "";
	static boolean live = true;
	static int historyIndex;
	static boolean muted = true;
	static Date historyFrozen = new Date();
	static SoundTheme currentSoundTheme;
	static Preferences preferences = Preferences.userNodeForPackage(UI.class);
	static enum DrawType { EDGES, PLUGIN, CALLS };
	static String statistics = "";
	static Graph graph;
	static Settings graphSettings = new Settings();
	static JobManager jobManager;
	

	public static void main(String args[]) {
		try {
			setupListener();
	    	createUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void setupListener() {
		new Thread(new Runnable() {
			public void run() {
				try{  
					ServerSocket serverSocket = new ServerSocket(6666);  
					Socket socket = serverSocket.accept();   
					DataInputStream dis=new DataInputStream(new BufferedInputStream(socket.getInputStream()));  
					eventStream = new DataOutputStream(socket.getOutputStream());
					while (true) {
						try {
							int command = dis.readInt();
							switch (command) {
							case Constants.EVENT_STATISTICS:
								statistics = (String)dis.readUTF();
								break;
							case Constants.EVENT_PLUGIN_DETAILS:
								// not handled
								break;
							case Constants.EVENT_JOB:
								handleJob((String)dis.readUTF());
								break;
							case Constants.EVENT_PLUGIN_TO_PLUGIN_CALL:
								String pluginToPlugin = (String)dis.readUTF();
								synchronized (called) {
									if (pluginToPlugin.indexOf(" ") != -1) {
										String[] pluginNames = pluginToPlugin.split(" ");
										String from = pluginNames[0];
										String to = pluginNames[1];
										switch (Plugin.detailLevel) {
										case FEATURE:
											// map fragments to plugins
											from = PluginRegistry.getPluginForFragment(from);
											to = PluginRegistry.getPluginForFragment(to);
											// map plugins to features
											from = PluginRegistry.getFeatureForPlugin(from);
											to = PluginRegistry.getFeatureForPlugin(to);
											break;
										case PLUGIN:
											// map fragments to plugins
											from = PluginRegistry.getPluginForFragment(from);
											to = PluginRegistry.getPluginForFragment(to);
											break;
										case FRAGMENT:
											// show all the detail possible
											break;
										default:
											break;
										}
										Plugin.called(from, to, graph);
										called.put(pluginToPlugin, true);
									}
								}
								break;
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

	static void handleJob(String jobDetails) {
		jobManager.addJob(jobDetails);
	}

	static void sendEvent(int command, String details) {
		try {
			eventStream.writeInt(command);
			eventStream.writeUTF(details);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static void createUI() {
		JFrame frame = new JFrame("Eclipse Cacophonia");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container mainContainer = frame.getContentPane();
		mainContainer.setLayout(new BorderLayout());
		
		Container header = createHeader();
		header.add(createThemeUI());
		header.add(createInstrumentSelector());
		header.add(new Label("|"));
		header.add(createFilterUI());
		mainContainer.add(header, BorderLayout.NORTH);

    	// fluxController.repaint();

		graph = new Graph(graphSettings);
		graph.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public PopupMenu createMenu(Component component) {
				return createPopupMenu(component);
			}
		});
		jobManager = new JobManager(graph);
		mainContainer.add(graph, BorderLayout.CENTER);
		
		frame.setSize(Constants.WIDTH, Constants.HEIGHT);
	    frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	static PopupMenu createPopupMenu(Component component) {
		Plugin plugin = (Plugin)component;
		PopupMenu menu = new PopupMenu();
		String name = plugin.fullName;
		MenuItem inspect = new MenuItem(plugin.beingInspected ? "Stop inspecting " + name : "Inspect " + name);
		menu.add(inspect);
		inspect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				plugin.toggleInspect();
				graph.remove(menu);
			}
		});
		MenuItem source = new MenuItem("Import this plugin as source");
		menu.add(source);
		source.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UI.sendEvent(Constants.EVENT_IMPORT_PLUGIN_FROM_SOURCE, plugin.name);
				graph.remove(menu);
			}
		});
		MenuItem repository = new MenuItem("Import this plugin from repository");
		menu.add(repository);
		repository.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				UI.sendEvent(Constants.EVENT_IMPORT_PLUGIN_FROM_REPOSITORY, plugin.name);
				graph.remove(menu);
			}
		});
		return menu;
	}

	static Component createFilterUI() {
		Container container = new Container();
		container.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		Button clear = new Button("clear");
		clear.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				Plugin.clear();
				graph.clear();
			}
		});
		container.add(clear);
		Button shake = new Button("shake");
		shake.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				graph.shake();
			}
		});
		container.add(shake);
		container.add(new Label("focus:"));
		TextField filter = new TextField(30);
		filter.addTextListener(new TextListener() {	
			@Override
			public void textValueChanged(TextEvent e) {
				UI.pluginFilter = filter.getText();
				UI.preferences.put("filter", ""+filter.getText());
				Plugin.focusUpdated();
			}
		});
		filter.setText(UI.pluginFilter = UI.preferences.get("filter", ""));
		container.add(filter);
		JCheckBox jobs = new JCheckBox("jobs", false);
		jobs.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				jobManager.enable(jobs.isSelected());
			}
		});
		container.add(jobs);
		
		JComboBox<String> levelSelector = new JComboBox<String>(new String[] { "Feature", "Plugin", "Fragment" });
		levelSelector.setMaximumRowCount(64);
		levelSelector.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				DetailLevel values[] = { DetailLevel.FEATURE, DetailLevel.PLUGIN, DetailLevel.FRAGMENT };
				Plugin.detailLevel = values[levelSelector.getSelectedIndex()];
				Plugin.clear();
				graph.clear();
			}
		});
		container.add(levelSelector);
		return container;
	}

	static Container createHeader() {
		Container header = new Container();
		header.setPreferredSize(new Dimension(800, 40));
		header.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 5));
		header.setPreferredSize(new Dimension(Constants.WIDTH,40));
		return header;
	}

	static Component createThemeUI() {
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
		selectSoundTheme(lastTheme);
		themeChooser.select(lastTheme);
		container.add(themeChooser);
		return container;
	}
	
	public static void selectSoundTheme(int index) {
		UI.currentSoundTheme = SoundTheme.themes[index];
		Plugin.updateInstruments();
		System.out.println("Theme changed to " + UI.currentSoundTheme.name);
	}
	
	static Component createInstrumentSelector() {
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

	public static void selectInstrument(int instrument) {
		instrumentSelector.setSelectedIndex(instrument + 1);
	}
	
}