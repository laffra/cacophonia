package cacophonia.ui.graph;

import java.awt.Graphics2D;

public interface PaintListener {
	
	void paintBefore(Graphics2D g);
	void paintAfter(Graphics2D g);
	
}
