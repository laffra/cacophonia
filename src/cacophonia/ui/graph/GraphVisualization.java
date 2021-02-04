package cacophonia.ui.graph;

import javax.swing.JFrame;

import cacophonia.ui.Plugin;


public class GraphVisualization {

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setSize(1200,1000);
		Graph graph = new Graph(new Settings());
		frame.add(graph);
		frame.setVisible(true);
		
		for (int n=0; n<10; n++) {
			for (int k=0; k<10; k++) {
				Plugin.called("plugin"+n, "plugin"+n+"-"+k, graph);
			}
		}
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					int n = (int) (Math.random() * 10);
					int k = (int) (Math.random() * 10);
					Plugin.called("plugin"+n, "plugin"+n+"-"+k, graph);
					try { Thread.sleep(300); } catch (InterruptedException e) { }
				}
			}
		}).start();
	}
}
