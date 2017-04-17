package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;

public class Link
{
	private float x1, x2, y1, y2;
	
	private Color color = Color.black;
	
	private float angle;
	
	private double bandwidth;
	
	private int delay;
	
	private Polygon area;
	
	private Rectangle infos;
	private boolean showInfos = false;
	
	public Link( float x1, float y1, float x2, float y2, float angle ) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		
		this.angle = angle;
		
		float offset = 10;
		
		area = new Polygon( new float[] {x1 + offset, y1 + offset, x2 + offset, y2 + offset, x2 - offset, y2 - offset, x1 - offset, y1 - offset} );
		infos = new Rectangle( 0, 0, 0, 0 );
	}
	
	public void setAngle( float val ) {
	    angle = val;
	}
	
	public float getAngle() {
	    return angle;
	}
	
	public boolean checkMouse( GameContainer gc, Input input ) {
		float mouseX = input.getMouseX();
		float mouseY = input.getMouseY();
		
		if (area.contains( mouseX, mouseY )) {
			showInfos = true;
			
			float offset = gc.getWidth()/80;
			infos.setLocation( mouseX + offset, mouseY - offset );
		} else {
			showInfos = false;
		}
		
		return showInfos;
	}
	
	public void drawLink( Graphics g, float offset ) {
		g.setColor( color );
		g.draw( area );
	}
	
	public void drawInfo( Graphics g ) {
		Font f = g.getFont();
		String value = bandwidth + "Mb/s, " + delay + "ms";
		infos.setSize( f.getWidth( value ), f.getHeight( value ) );
		
		if (showInfos) {
			g.setColor( Color.black );
			g.draw( infos );
			g.setColor( Color.blue );
			g.fill( infos );
			g.setColor( Color.black );
			g.drawString( value, infos.getX(), infos.getY() );
		}
	}
}
