package org.openmeetings.webstart.gui;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import javax.swing.event.MouseInputAdapter;

import org.openmeetings.webstart.beans.VirtualScreenBean;

public class VirtualScreenXMouseListener extends MouseInputAdapter  {
	
	private double x = 0;
	
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		StartScreen.instance.t.setCursor( Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR) ) ;
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		StartScreen.instance.t.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) ) ;
	}
	
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		VirtualScreen.instance.showWarning=false;
		this.x = e.getX();
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		VirtualScreen.instance.showWarning=true;
	}
	
	public void mouseDragged(MouseEvent e) {
		double newX = e.getX();
		int delta = Long.valueOf(Math.round(this.x-newX)).intValue();
		int newXPosition = VirtualScreenBean.vScreenSpinnerX-delta;
		int newWidth = VirtualScreenBean.vScreenSpinnerWidth+delta;
		
		//System.out.println(newX+" "+newXPosition+" "+newWidth);
		if (newXPosition>=0 && newWidth>=0) {
			VirtualScreen.instance.doUpdateBounds=false;
			StartScreen.instance.jVScreenXSpin.setValue(newXPosition);
			StartScreen.instance.jVScreenWidthSpin.setValue(newWidth);
			VirtualScreen.instance.doUpdateBounds=true;
			VirtualScreen.instance.updateVScreenBounds();
			
		}

	}	

}
