package cacophonia.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import cacophonia.Constants;

/**
 * A component that shows the plugins active in the past and allows the user to select a point in time to explore.
 */
class FluxController extends JPanel {
	private static final int TIME_TRAVELER_WIDTH = 120;
	private static final int TIME_TRAVELER_HEIGHT = 30;
	private int value, highlight;
	private ChangeListener listener;
	private Image drawing = new BufferedImage(TIME_TRAVELER_WIDTH, TIME_TRAVELER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
	private long lastDraw;
	
	
	public FluxController() {
		setBackground (Color.BLACK);  
	    setPreferredSize(new Dimension(TIME_TRAVELER_WIDTH, TIME_TRAVELER_HEIGHT));
	    addMouseListener(new MouseAdapter() {
	    	public void mouseClicked(MouseEvent e) {
	    		value = e.getX() * Constants.HISTORY_SIZE / TIME_TRAVELER_WIDTH;
	    		repaint();
    			listener.stateChanged(null);
	    	}
	    });
	    addMouseMotionListener(new MouseAdapter() {
	    	public void mouseMoved(MouseEvent e) {
	    		highlight = e.getX() * Constants.HISTORY_SIZE / TIME_TRAVELER_WIDTH;
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
	public void repaint() { 
		long now = System.currentTimeMillis();
		if (now - lastDraw < 500) return;
		lastDraw = now;
		super.repaint();
	}
	public void paintComponent(Graphics graphics) { 
		Graphics2D g = (Graphics2D) drawing.getGraphics();
		drawHistory(g);
        drawMarker(g);
        drawHighlight(g);
    	graphics.drawImage(drawing, 0, 0, this);
	}
	private void drawHistory(Graphics2D g) {
	    int index = 0;
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, TIME_TRAVELER_WIDTH, TIME_TRAVELER_HEIGHT);
    	g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Color.RED);
		synchronized(UI.history) { 
			for (HashMap<String, Integer> scores: UI.history) {
				if (scores.size() > 0) {
					int x = TIME_TRAVELER_WIDTH * index / Constants.HISTORY_SIZE;
					int h = Math.max(TIME_TRAVELER_HEIGHT - 30, 4 * (int)Math.log(scores.size()));
					g.drawLine(x, TIME_TRAVELER_HEIGHT - h, x, TIME_TRAVELER_HEIGHT);
					if (hasSound(scores)) {
						g.setColor(Color.YELLOW);
						g.drawRect(x - 1, TIME_TRAVELER_HEIGHT - h - 1, 3, 3);
						g.setColor(Color.RED);        		
					}
				}
				index += 1;
			}
		}
	}
	private void drawMarker(Graphics2D g) {
    	drawBar(g, Color.ORANGE, value);
	} 
	private void drawHighlight(Graphics2D g) {
		drawBar(g, Color.WHITE, highlight);
    	highlight = 0;
	} 
	private void drawBar(Graphics2D g, Color color, int location) {
    	g.setColor(color);
    	g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    	int x = TIME_TRAVELER_WIDTH * location / Constants.HISTORY_SIZE - 1;
    	g.drawLine(x, 0, x, TIME_TRAVELER_HEIGHT);
	} 
	private boolean hasSound(Map<String,Integer> scores) {
		for (Map.Entry<String,Integer> entry : scores.entrySet()) {
	    	String key = entry.getKey();
	    	int value = entry.getValue();
	    	if (value == 0) continue;
	    	String pluginNames[] = key.split(" ");
	    	Plugin from = Plugin.get(pluginNames[0]);
	    	Plugin to = Plugin.get(pluginNames[1]);
	    	if (from.instrument != -1) return true;
	    	if (to.instrument != -1) return true;
	    }
		return false;
	}
}