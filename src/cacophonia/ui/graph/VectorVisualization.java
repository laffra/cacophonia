package cacophonia.ui.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;
import javax.swing.JFrame;

class VectorVisualization {
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setTitle("Test for Vector - Vector.getX(), Vector.getY(), and Vector.add(Vector)");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1200,800);
	    frame.setLocationRelativeTo(null);
		Vector vector1 = new Vector(200, 0);
		Vector vector2 = new Vector(200, 90);
		final double angle[] = { 0 };
		class Drawing extends JComponent {
			public void paintComponent(Graphics graphics) {
				Graphics2D g = (Graphics2D)graphics;
				g.setStroke(new BasicStroke(2));
				g.setColor(Color.BLACK);
				g.fillRect(0,  0, 1200, 800);

				g.setColor(Color.YELLOW);
				g.drawOval(200, 200, 400, 400);
				g.setColor(Color.WHITE);
				g.drawString("angle=" + angle[0], 300, 100);

				drawVector(g, vector1, Color.RED, "Vector 1");
				drawVector(g, vector2, Color.BLUE, "Vector 2");
				
				Vector vector3 = new Vector(vector1.force, vector1.angle);
				vector3.add(vector2);
				String expectation = "Vector 1 and 2 added, should be in between vector 1 and 2";
				drawVector(g, vector3, Color.ORANGE, expectation);
			}
			
			void drawVector(Graphics g, Vector vector, Color color, String label) {
				g.setColor(color);
				int x = 400 + (int)vector.getX();
				int y = 400 - (int)vector.getY();
				g.drawLine(400, 400, x, y);
				
				g.setColor(Color.WHITE);
				g.drawString(vector.toString(), x, y);
				g.drawString(label, x, y - 13);
			}
		}
		Drawing drawing = new Drawing();
		drawing.setSize(500, 500);
		frame.add(drawing);
		frame.setVisible(true);
		new Thread(new Runnable() {
			public void run() {
				while (true) {
					for (int n=0; n<360; n++) {
						angle[0] += 1;
						vector1.angle++;
						vector2.angle++;
						drawing.repaint();
						try { Thread.sleep(10); } catch (Exception e) { }
					}
					reset();
					for (int n=0; n<360; n++) {
						angle[0] += 1;
						vector2.angle++;
						drawing.repaint();
						try { Thread.sleep(10); } catch (Exception e) { }
					}
					reset();
				}
			}
			void reset() {
				angle[0] = 0;
				vector1.angle = 0;
				vector2.angle = 90;
			}
		}).start();
	}
}
	