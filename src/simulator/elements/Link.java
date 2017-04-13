package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;

public class Link
{
	private float x1, x2, y1, y2;
	
	private Color color = Color.black;
	
	private float angle;
	
	private double bandwidth;
	
	private int delay;
	
	public Link( float x1, float y1, float x2, float y2, float angle ) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
		
		this.angle = angle;
	}
	
	public void setAngle( float val ) {
	    angle = val;
	}
	
	public float getAngle() {
	    return angle;
	}
	
	public void draw( Graphics g, float offset ) {
		g.setColor( color );
		g.drawLine( x1, y1 - offset, x2, y2 - offset );
		g.drawLine( x1, y1 + offset, x2, y2 + offset );
		
		Font f = g.getFont();
		String value = bandwidth + "Mb/s, " + delay + "ms";
		float X = x1 + (x2 - x1)/2 - f.getWidth( value )/2;
		float Y = y1 + (y2 - y1)/2 - f.getHeight( value )/2;
		
		g.rotate( X + f.getWidth( value )/2, Y + f.getHeight( value )/2, angle );
		g.drawString( value, X, Y );
		g.resetTransform();
	}
}
