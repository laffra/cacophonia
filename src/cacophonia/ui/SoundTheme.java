package cacophonia.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SoundTheme {
	String name;
	HashMap<String,Integer> pluginToInstrument = new HashMap<>();
	static SoundTheme[] themes;

	
	public SoundTheme(String name) {
		this.name = name;
		loadTheme();
	}
	
	void loadTheme() {
		String assignments[] = UI.preferences.get(name, "").split("#");
		for (String assignment : assignments) {
			if (assignment.length() == 0) continue;
			String[] keyValue = assignment.split("=");
			pluginToInstrument.put(keyValue[0], Integer.parseInt(keyValue[1]));
		}
		Plugin.updateInstruments();
	}
	
	void saveTheme() {
		List<String> assignments = new ArrayList<String>();
		for (Map.Entry<String,Integer> entry : pluginToInstrument.entrySet()) {
			assignments.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}
		UI.preferences.put(name, String.join("#", assignments));
	}
	
	int getInstrument(String pluginName) {
		if (name.equals("Cacophonia")) {
			return Math.abs(pluginName.hashCode()) % 128;
		}
		return pluginToInstrument.getOrDefault(pluginName, -1);
	}
	
	int getNote(String pluginName) {
		// Return a note related to the plugin's name, random, but the same each run
		return 80 + pluginName.hashCode() % 48;
	}
	
	static {
		UI.currentSoundTheme = new SoundTheme("Cacophonia");
		themes = new SoundTheme[] {
				UI.currentSoundTheme,
				new SoundTheme("Theme 1"),
				new SoundTheme("Theme 2"),
				new SoundTheme("Theme 3"),
		};
	}

	public void setInstrument(String pluginName, int instrument) {
		pluginToInstrument.put(pluginName, instrument);
		saveTheme();
	}
	
	public String getName() {
		return name;
	}
}