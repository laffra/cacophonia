package cacophonia;

public class Constants {
	public static final int EVENT_EXIT = 0;
	public static final int EVENT_INSPECT_PLUGIN = 1;
	public static final int EVENT_UN_INSPECT_PLUGIN = 2;
	public static final int EVENT_IMPORT_PLUGIN_FROM_SOURCE = 3;
	public static final int EVENT_IMPORT_PLUGIN_FROM_REPOSITORY = 4;
	public static final int EVENT_PLUGIN_DETAILS = 5;
	public static final int EVENT_PLUGIN_TO_PLUGIN_CALL = 6;
	public static final int EVENT_STATISTICS = 7;
	public static final int EVENT_JOB = 8;
	
	public static final int WIDTH = 1200;
	public static final int HEIGHT = 1000;
	public static final int CENTER_X = WIDTH / 2;
	public static final int CENTER_Y = HEIGHT / 2 - 100;

	public static final int MARGIN = 5;
	public static final int PLUGIN_SIZE = 60;
	public static final int FONT_SIZE = 14;
	public static final int FONT_SIZE_SMALL = 9;
	
	public static final int REDRAW_DELAY = 150;
	
	public static final int MUTE_DELAY = 500;
	public static final int HEART_BEAT = 3;
	public static final int MAX_AGE = 5;

	public static final int JOBS_SHOWN_COUNT = 70;
}
