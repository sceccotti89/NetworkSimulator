package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Circle;

public class Node
{
	private long ID_from, ID_to;
	
	private Color color;
	
	private Circle node;
	
	final private float ray = 25;
	
	private Link link = null;
	
	public Node( final float x, final float y, final long ID_from, final long ID_to, final Color color ) {
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		
		node = new Circle( x, y, ray );
	}
	
	public long getIDFrom() {
		return ID_from;
	}
	
	public long getIDTo() {
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
	
	public int getCenterX() {
		return (int) node.getCenterX();
	}
	
	public int getCenterY() {
		return (int) node.getCenterY();
	}
	
	public Circle getArea(){
		return node;
	}
	
	public float getAngle() {
	    return link.getAngle();
	}
	
	private float calculateAngle( float x1, float y1, float x2, float y2 ) {
	    float catetum1 = x2 - x1, catetum2 = y2 - y1;
	    //float ipo = (float) Math.sqrt( catetum1 * catetum1 + catetum2*catetum2 );
	    float gamma = (float) Math.atan( catetum2/catetum1 );
	    return (float) ((Math.PI/2 - gamma)*180/Math.PI);
	}
	
	public void createLink( GameContainer gc, float x1, float y1, float x2, float y2, Color color ) {
	    link = new Link( gc, x1, y1, x2, y2, calculateAngle( x1, y1, x2, y2 ) );
	}
	
	public float getLinkLenght() {
		return link.getLenght();
	}
	
	public float getRay() {
		return ray;
	}
	
	public void update( GameContainer gc ) {
		if (link != null) {
			link.checkMouse( gc );
		}
	}
	
	public void drawNode( Graphics g ) {
	    if (link != null) {
	        link.drawLink( g );
	    }
		
		g.setColor( color );
		g.fill( node );
		
		g.setColor( Color.black );
		g.draw( node );
		
		Font f = g.getFont();
		g.setColor( Color.white );
		g.drawString( ID_from + "", node.getCenterX() - f.getWidth( ID_from + "" )/2, node.getCenterY() - f.getHeight( ID_from + "" )/2 );
	}
	
	public void drawInfo( Graphics g ) {
		if (link != null) {
	        link.drawInfo( g );
	    }
	}
}
