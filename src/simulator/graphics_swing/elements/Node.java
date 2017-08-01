package simulator.graphics_swing.elements;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import simulator.topology.NetworkLink;

public class Node
{
    private long nodeID;
    private Color color;
    
    private Rectangle node;
    final private int ray = 25;
    
    final private long delay;
    final private String name;
    
    private List<Link> links;
	private boolean removable;
    
    public Node( final double x, final double y, final long nodeID, final String name, final long delay, final Color color )
    {
        this.nodeID = nodeID;
        this.color = color;
        
        this.delay = delay;
        this.name = name;
        
        node = new Rectangle( (int) x, (int) y, ray, ray );
        
        links = new ArrayList<>();
    }
    
    public long getNodeID() {
        return nodeID;
    }
    
    public Color getColor() {
        return color;
    }
    
    public int getCenterX() {
        return (int) node.getCenterX();
    }
    
    public int getCenterY() {
        return (int) node.getCenterY();
    }
    
    public Rectangle getArea() {
        return node;
    }
    
    public Float getAngle( final long destID ) {
        for (Link link: links) {
            if (link.getDestNode().getNodeID() == destID)
                return link.getAngle();
        }
        
        return null;
    }
    
    public int numberLinks() {
        return links.size();
    }
    
    private float angleValutation( final double x1, final double y1, final float x2, final float y2 )
    {
        double m = (y2 - y1)/(x2 - x1);
        
        if (y1 == y2) {
            if (x1 <= x2) return  0;
            else return  -180;
        }
        
        if (y1 > y2) {
            if (x1 < x2) {
                return  (float) (90 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            } else if (x1 > x2) {
                return (float) (270 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            }
        } else {
            if (x1 < x2) {
                return (float) (90 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            } else if (x1 > x2) {
                return (float) (270 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            }
        }
        
        return 0;
    }

    private float calculateAngle( final float x1, final float y1, final float x2, final float y2 ) {
        return angleValutation( x1, y1, x2, y2 );
    }
    
    public void addLink( final Node dest, final double bandwidth, final long delay, final int width, final int height, final String type ) {
        float angle = calculateAngle( getCenterX(), getCenterY(), dest.getCenterX(), dest.getCenterY() );
        links.add( new Link( this, dest, bandwidth, delay, angle, width, height, type ) );
    }
    
    public Float getLinkLenght( final long destID ) {
        for (Link link: links) {
            if (link.getDestNode().getNodeID() == destID) {
                return link.getLenght();
            }
        }
        
        return null;
    }
    
    public float getRay() {
        return ray;
    }
    
    public long getDelay() {
        return delay;
    }
    
    public String getName() {
        return name;
    }
    
    public void setLinkPosition( Link link, Node source ) {
    	for(Link linked: links) {
    		if (linked.getDestNode() == source) {
    			float angle = angleValutation( node.getCenterX(), node.getCenterY(), linked.getDestNode().getCenterX(), linked.getDestNode().getCenterY() );
        		linked.setPosition( this, angle );
    		}
    	}
    }
    
    public boolean checkCollision( final int mouseX, final int mouseY ) {
    	return node.contains( mouseX, mouseY );
    }
    
    public boolean checkLinks( final Node source ) {
        for (Link link: links) {
            if (link.getDestNode().equals( source )) {
                return true;
            }
        }
        
        return false;
    }
    
    public void setLinkAvailable() {
        for (Link link: links) {
            link.setAvailable();
        }
    }
    
    public void removeLink( Node node ) {
    	if (node != null) {
	    	for (Link link: links) {
	            if (link.getDestNode().equals( node )) {
	            	links.remove( link );
	            	return;
	            }
	        }
    	} else {
    		links.clear();
    	}
    }
    
    public void setRemovable() {
    	removable = !removable;
    }
    
    public Node clone( final Node dest, final int width, final int height ) {
    	Node tmp = new Node( node.getCenterX(), node.getCenterY(), nodeID, name, delay, color );
		tmp.addLink( dest, 0, 0, width, height, NetworkLink.BIDIRECTIONAL );
		return tmp;
    }
    
    public void update()
    {
    	// TODO inserire lo spostamento di un nodo
        
    }
    
    public void drawLinks( final Graphics2D g ) {
        for (Link link: links) {
            link.render( g );
        }
    }
    
    public void drawNode( final Graphics2D g ) {
        g.setColor( color );
        g.fill( node );
        
        g.setColor( Color.black );
        g.draw( node );
        
        g.setColor( Color.white );
        FontMetrics font = g.getFontMetrics();
        double width  = font.stringWidth( nodeID + "" );
        double height = font.getHeight();
        g.drawString( nodeID + "",
                     (int) (node.getCenterX() - width/2),
                     (int) (node.getCenterY() - height/2) );
    }
}