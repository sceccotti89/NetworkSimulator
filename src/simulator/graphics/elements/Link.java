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
	
	private Polygon area;
	
	private Rectangle infos;
	private boolean showInfos = false;
	
	private final float offset;
	
	public Link( GameContainer gc, float x1, float y1, float x2, float y2, float angle ) {
		
		this.angle = angle;
		
		offset = gc.getWidth()/80;
		
		//System.out.println( "ID = " + ID_from );
		
		System.out.println( "X1 + off = " + (x1 + offset) );
		System.out.println( "X1 - off = " + (x1 - offset) );
		System.out.println( "X2 + off = " + (x2 + offset) );
		System.out.println( "X2 - off = " + (x2 - offset) );
		
		System.out.println( "Y1 + off = " + (y1 + offset) );
		System.out.println( "Y1 - off = " + (y1 - offset) );
		System.out.println( "Y2 + off = " + (y2 + offset) );
		System.out.println( "Y2 - off = " + (y2 - offset) );
		
		area = new Polygon( new float[] {x1 + offset, y1 + offset, x2 + offset, y2 + offset, x2 - offset, y2 - offset, x1 - offset, y1 - offset} );
		infos = new Rectangle( 0, 0, 0, 0 );
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
			g.setColor( Color.lightGray );
			g.fill( infos );
			g.setColor( Color.black );
			g.drawString( value, infos.getX(), infos.getY() );
		}
	}
}
