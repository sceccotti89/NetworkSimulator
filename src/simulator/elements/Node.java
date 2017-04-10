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
	
	private Link link = null;
	
	private float offset;
	
	public Node( final float x, final float y, final int ID_from, final int ID_to, final Color color, float offset ) {
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		this.offset = offset;
		
		node = new Circle( x, y, ray );
	}
	
	public void draw( Graphics g ) {
	    if (link != null) {
	        link.draw( g, offset );
	    }
		
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
	
	public float getAngle() {
	    return link.getAngle();
	}
	
	private float calculateAngle( float x1, float y1, float x2, float y2 ) {
	    float catetum1 = Math.abs( x2 - x1 ), catetum2 = Math.abs( y1 - y2 );
	    //float ipo = (float) Math.sqrt( catetum1 * catetum1 + catetum2*catetum2 );
	    float gamma = (float) Math.atan( catetum2/catetum1 );
	    System.out.println( "CATETO2 = " + catetum2 );
	    return (float) ((Math.PI/2 - gamma)*180/Math.PI);
	}
	
	public void createLink( float x1, float y1, float x2, float y2, Color color ) {
	    if (x1 != x2 && y1 != y2) {
            //System.out.print( "ANGOLO != 0" );
	        link = new Link( x1, y1, x2, y2, color, calculateAngle( x1, x2, y1, y2 ) );
	    } else {
	        //System.out.print( "ANGOLO = 0" );
	        link = new Link( x1, y1, x2, y2, color, 0 );
	    }
	}
}
