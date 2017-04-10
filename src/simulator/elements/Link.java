package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;

public class Link
{
	private float x1, x2, y1, y2;
	
	private Color color;
	
	private float angle;
	
	public Link( float x1, float y1, float x2, float y2, Color color, float angle ) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		
		this.color = color;
		this.angle = angle;
	}
	
	public void setAngle( float val ) {
	    angle = val;
	}
	
	public float getAngle() {
	    return angle;
	}
	
	public void draw( Graphics g, float offset ) {
		g.drawGradientLine( x1, y1 - offset, color, x2, y2 - offset, color );
		g.drawGradientLine( x1, y1 + offset, color, x2, y2 + offset, color );
	}
}
