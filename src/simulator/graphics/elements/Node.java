package simulator.graphics.elements;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Circle;

public class Node
{
	private long nodeID;
	
	private Color color;
	
	private Circle node;
	
	final private float ray = 25;
	
	private List<Link> links;
	
	public Node( final float x, final float y, final long nodeID, final Color color ) {
		this.nodeID = nodeID;
		this.color = color;
		
		node = new Circle( x, y, ray );
		
		links = new ArrayList<>();
	}
	
	public long getNodeID() {
		return nodeID;
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
	
	public Float getAngle( final long destID ) {
		for (Link link: links) {
			if (link.getDestID() == destID)
				return link.getAngle();
		}
		
		return null;
	}
	
	public int numberLinks() {
		return links.size();
	}
	
	private float angleValutation( float x1, float y1, float x2, float y2, float cateto1, float cateto2, float ipo ) {
		float gamma;
		if (y1 == y2) {
			if (x1 < x2) {
				return 0;
			} else {
				return 180;				
			}
		} else if (x1 == x2) {
			if (y1 < y2) {
				return 90;
			} else {
				return 270;				
			}
		}
		if (y1 > y2) {
			gamma = (float) Math.asin( cateto2/ipo );
			if (x1 < x2) {
				System.out.println( "ANGOLO = " + (Math.PI/2 - gamma)*180/Math.PI );
				return (float) (0 - (Math.PI/2 - gamma)*180/Math.PI);
			} else if (x1 > x2) {
				return (float) (270 - (Math.PI/2 - gamma)*180/Math.PI);
			}
		} else if (y1 < y2) {
			gamma = (float) Math.asin( cateto1/ipo );
			if (x1 < x2) {
				return (float) ((Math.PI/2 - gamma)*180/Math.PI);
			} else if (x1 > x2) {
				return (float) (180 - (Math.PI/2 - gamma)*180/Math.PI);
			} 
		}
		
		return 0;
	}
	
	// TODO DA RIVEDERE QUESTA PARTE
	private float calculateAngle( float x1, float y1, float x2, float y2 ) {
		float cateto1 = Math.abs( x2 - x1 ), cateto2 = Math.abs( y2 - y1 );
	    float ipo = (float) Math.sqrt( cateto1 * cateto1 + cateto2 * cateto2 );
		
	    return angleValutation( x1, y1, x2, y2, cateto1, cateto2, ipo );
	}
	
	public void addLink( long destID, float x1, float y1, float x2, float y2, int width, int height ) {
	    links.add( new Link( nodeID, destID, x1, y1, x2, y2, calculateAngle( x1, y1, x2, y2 ), width, height ) );
	}
	
	public Float getLinkLenght( final long destID ) {
		for (Link link: links) {
			System.out.println( "ECCHIME " + link.getDestID() );
			if (link.getDestID() == destID) {
				return link.getLenght();
			}
		}
		
		return null;
	}
	
	public float getRay() {
		return ray;
	}
	
	public void update( GameContainer gc ) {
		for (Link link: links) {
			link.checkMouse( gc );
		}
	}
	
	public void drawLinks( Graphics g ) {
		for (Link link: links) {
	        link.drawLink( g );
	    }
	}
	
	public void drawNode( Graphics g ) {
	    g.setColor( color );
		g.fill( node );
		
		g.setColor( Color.black );
		g.draw( node );
		
		Font f = g.getFont();
		g.setColor( Color.white );
		g.drawString( nodeID + "", node.getCenterX() - f.getWidth( nodeID + "" )/2, node.getCenterY() - f.getHeight( nodeID + "" )/2 );
	}
	
	public void drawInfo( Graphics g ) {
		for (Link link: links) {
	        link.drawInfo( g );
	    }
	}
}
