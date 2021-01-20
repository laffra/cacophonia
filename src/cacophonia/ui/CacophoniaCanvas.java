package cacophonia.ui;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import cacophonia.Constants;

/**
 * The canvas used to draw plugins on. Mouse clicks enable/disable sounds.
 */
class CacophoniaCanvas extends Canvas {  
	public CacophoniaCanvas() { 
	    setBackground (Color.BLACK);  
	    setSize(WIDTH, HEIGHT);  
	    addMouseListener(new MouseAdapter() {
	    	@Override
	    	public void mouseClicked(MouseEvent e) {
	    		if (Plugin.select(e.getX(), e.getY())) {
	    			super.mouseClicked(e);
	    			if (e.getButton() != 1) {
	    				Plugin plugin = Plugin.selectedPlugin;
	    			    PopupMenu menu = new PopupMenu();
    					MenuItem inspect = new MenuItem(plugin.beingInspected ? "Stop inspecting this plugin" : "Inspect this plugin");
	    			    menu.add(inspect);
    					inspect.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								plugin.toggleInspect();
								CacophoniaCanvas.this.remove(menu);
							}
						});
    					MenuItem source = new MenuItem("Import this plugin as source");
	    			    menu.add(source);
	    			    source.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								UI.sendEvent(Constants.IMPORT_PLUGIN_FROM_SOURCE, plugin.name);
								CacophoniaCanvas.this.remove(menu);
							}
						});
    					MenuItem repository = new MenuItem("Import this plugin from repository");
	    			    menu.add(repository);
	    			    repository.addActionListener(new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								UI.sendEvent(Constants.IMPORT_PLUGIN_FROM_REPOSITORY, plugin.name);
								CacophoniaCanvas.this.remove(menu);
							}
						});
    					CacophoniaCanvas.this.add(menu);
	    				menu.show(CacophoniaCanvas.this, e.getX(), e.getY());
	    			}
	    		}
	    	}
		});
	}  
    public void paint(Graphics g) { 
    	g.drawImage(UI.drawing, 0, 0, this);
	}  
}