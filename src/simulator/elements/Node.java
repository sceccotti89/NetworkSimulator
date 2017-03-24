package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Circle;

public class Node
{
	private String ID_from, ID_to;
	
	private Color color;
	
	private Circle node;
	
	final private float ray = 25;
	
	public Node( final float x, final float y, final String ID_from, final String ID_to, final Color color ){
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		
		node = new Circle( x, y, ray );
	}
	
	public void draw( Graphics g ){
		g.setColor( color );
		g.fill( node );
	}
	
	public String getIDFrom(){
		return ID_from;
	}
	
	public String getIDTo(){
		return ID_to;
	}
}
