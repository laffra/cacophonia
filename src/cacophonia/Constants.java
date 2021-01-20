package cacophonia;

public class Constants {
	public static final int EXIT = 0;
	public static final int INSPECT_PLUGIN = 1;
	public static final int UN_INSPECT_PLUGIN = 2;
	public static final int IMPORT_PLUGIN_FROM_SOURCE = 3;
	public static final int IMPORT_PLUGIN_FROM_REPOSITORY = 4;
	
	public static final int WIDTH = 1200;
	public static final int HEIGHT = 1200;
	public static final int PLUGINS_PER_ROW = 14;
	public static final int MARGIN = 3;
	public static final int PLUGIN_SIZE = WIDTH / PLUGINS_PER_ROW;
	
	public static final int REDRAW_DELAY = 150;
	public static final int HISTORY_SECONDS = 60;
	public static final int HISTORY_SAMPLES_PER_SECOND = 1000 / REDRAW_DELAY;
	public static final int HISTORY_SIZE = HISTORY_SECONDS * HISTORY_SAMPLES_PER_SECOND;
	
	public static final int HISTORY_SLIDER_SIZE = HISTORY_SIZE / 2;
	public static final int HISTORY_INCREMENT = 2;
	
	public static final int MUTE_DELAY = 1500;
	public static final int HEART_BEAT = 3;
}
