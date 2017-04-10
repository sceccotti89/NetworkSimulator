package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Circle;

public class Node
{
	private int ID_from, ID_to;
	
	private Color color;
	
	private Circle node;
	
	final private float ray = 25;
	
	private Link link;
	
	private float offset;
	
	public Node( final float x, final float y, final int ID_from, final int ID_to, final Color color, float offset ) {
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		this.offset = offset;
		
		node = new Circle( x, y, ray );
	}
	
	public void draw( Graphics g ) {
		link.draw( g, offset);
		
		g.setColor( color );
		g.fill( node );
		
		g.setColor( Color.black );
		g.draw( node );
	}
	
	public int getIDFrom() {
		return ID_from;
	}
	
	public int getIDTo() {
		return ID_to;
	}
	
	public Color getColor() {
		return color;
	}
	
	public float getX() {
		return node.getX();
	}
	
	public float getY() {
		return node.getY();
	}
	
	public float getCenterX() {
		return node.getCenterX();
	}
	
	public float getCenterY() {
		return node.getCenterY();
	}
	
	public Circle getArea(){
		return node;
	}
	
	public void creatLink( Link link ) {
		this.link = link;
	}
}
