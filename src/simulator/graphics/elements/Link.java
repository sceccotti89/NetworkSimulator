package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;

public class Link
{
	private Color color = Color.black;
	
	private float angle;
	
	private double bandwidth;
	
	private int delay;
	
	private Rectangle area;
	
	private Rectangle infos;
	private boolean showInfos = false;
	
	private final float offset;
	
	private float lenght;
	
	private long fromID, destID;
	
	public Link( long fromID, long destID, float x1, float y1, float x2, float y2, float angle, int width, int height ) {
		
		this.fromID = fromID;
		this.destID = destID;
		
		this.angle = angle;

		offset = width/80;
		
		lenght = calculateLenght( x1, y1, x2, y2 );
		
		area = new Rectangle( x1, y1 - offset, calculateLenght( x1, y1, x2, y2 ), offset * 2 );
		
		/*if (y1 == y2) {
			area = new Polygon( new float[] {x1, y1 + offset, x2, y2 + offset, x2, y2 - offset, x1, y1 - offset} );
		} else {
			area = new Polygon( new float[] {x1 + offset, y1, x2 + offset, y2, x2 - offset, y2, x1 - offset, y1} );
		}*/
		
		infos = new Rectangle( 0, 0, 0, 0 );
	}
	
	public long getFromID() {
		return fromID;
	}
	
	public long getDestID() {
		return destID;
	}
	
	public float calculateLenght( float x1, float y1, float x2, float y2 ) {
		return (float) Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) );
	}
	
	public float getLenght() {
		return lenght;
	}
	
	public void setAngle( float val ) {
	    angle = val;
	}
	
	public float getAngle() {
	    return angle;
	}
	
	public boolean checkMouse( GameContainer gc ) {
		float mouseX = gc.getInput().getMouseX();
		float mouseY = gc.getInput().getMouseY();
		
		if (area.contains( mouseX, mouseY )) {
			showInfos = true;
			
			infos.setLocation( mouseX + offset, mouseY - offset );
		} else {
			showInfos = false;
		}
		
		return showInfos;
	}
	
	public void drawLink( Graphics g ) {
		g.rotate( area.getX(), area.getY() + offset, angle );
		g.setColor( color );
		g.draw( area );
		g.resetTransform();
	}
	
	public void drawInfo( Graphics g ) {
		Font f = g.getFont();
		String value = bandwidth + "Mb/s, " + delay + "ms";
		infos.setSize( f.getWidth( value ), f.getHeight( value ) );
		
		if (showInfos) {
			g.setColor( Color.black );
			g.draw( infos );
			g.setColor( Color.lightGray );
			g.fill( infos );
			g.setColor( Color.black );
			g.drawString( value, infos.getX(), infos.getY() );
		}
	}
}
