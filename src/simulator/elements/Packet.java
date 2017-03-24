package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Packet
{
	private String ID_from, ID_to;
	
	private Color color;
	
	private Rectangle pack;
	
	private float width;
	
	public Packet( final GameContainer gc, final float x, final float y, final String ID_from, String ID_to, Color color ){
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		
		width = gc.getWidth()/80;
		
		pack = new Rectangle( x, y, width, width );
	}
	
	public void draw( final Graphics g ){
		g.setColor( color );
		g.draw( pack );
	}
	
	public void setIDFrom( final String IDFrom ){
		ID_from = IDFrom;
	}
	
	public void setIDTo( final String IDTo ){
		ID_to = IDTo;
	}
}
