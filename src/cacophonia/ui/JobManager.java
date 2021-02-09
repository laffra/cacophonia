package cacophonia.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import cacophonia.Constants;
import cacophonia.ui.graph.Graph;
import cacophonia.ui.graph.PaintListener;

public class JobManager {
	static Composite transparancies[] = {
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f),
			AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f),
	};

	List<String> jobs = new ArrayList<String>();
	boolean enabled;

	public JobManager(Graph graph) {
		for (int n=0; n<Constants.JOBS_SHOWN_COUNT; n++) jobs.add("");
		graph.addPaintListener(new PaintListener() {
			
			@Override
			public void paintBefore(Graphics2D g) {
			}
			
			@Override
			public void paintAfter(Graphics2D g) {
				if (!enabled) return;
				try {
					paintJobs(g);
				} catch (ConcurrentModificationException e) {
					// a new job arrived while painting, ignore for performance
				}
			}
			
			private void paintJobs(Graphics2D g) {
				int fontSize = (int)(UI.currentScale * Constants.FONT_SIZE);
				Font font = new Font("Courier New", Font.PLAIN, fontSize);
				g.setFont(font);
				g.setColor(Color.GRAY);
				g.setComposite(Plugin.opaque);
				int n=0;
				for (String jobDetails: jobs) {
					int y = Constants.HEIGHT - 100 - jobs.size() * fontSize + fontSize * n++;
					if (y > 60) {
						int transparancyIndex = n / (jobs.size() / (transparancies.length) + 1);
						Composite transparancy = transparancies[transparancyIndex];
						g.setComposite(transparancy);
						g.drawString(jobDetails, 10, y);
					}
				}
			}
		});
	}

	public void addJob(String jobDetails) {
		jobs.add(jobDetails);
		if (jobs.size() > Constants.JOBS_SHOWN_COUNT) {
			jobs.remove(0);
		}
	}

	public void enable(boolean enabled) {
		this.enabled = enabled;
	}

}
