package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Packet
{
	private int ID_from, ID_to;
	
	private Color color;
	
	private Rectangle pack;
	
	private float width;
	
	public Packet( final GameContainer gc, final float x, final float y, final int ID_from, int ID_to, Color color ) {
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		
		width = gc.getWidth()/80;
		
		pack = new Rectangle( x, y - width/2, width, width );
	}
	
	public void draw( final Graphics g ) {
		g.setColor( color );
		g.fill( pack );
	}
	
	public void setIDFrom( final int IDFrom ) {
		ID_from = IDFrom;
	}
	
	public void setIDTo( final int IDTo ) {
		ID_to = IDTo;
	}
	
	public Rectangle getArea(){
		return pack;
	}
	
	public void setColor( Color color ){
		this.color = color;
	}
	
	public int getIDFrom(){
		return ID_from;
	}
	
	public int getIDTo(){
		return ID_to;
	}
}
