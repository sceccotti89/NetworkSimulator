
package simulator.graphics_swing.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
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
        
        node = new Rectangle( (int) x, (int) y, ray * 2, ray * 2 );
        
        links = new ArrayList<>();
    }
    
    public long getID() {
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
    
    public int numberLinks() {
        return links.size();
    }
    
    public float calculateAngle( final Node dest ) {
        return calculateAngle( getCenterX(), getCenterY(),
                               dest.getCenterX(), dest.getCenterY() );
    }
    
    public float calculateAngle( final double x1, final double y1, final float x2, final float y2 )
    {
        double m = (y2 - y1)/(x2 - x1);
        
        if (y1 == y2) {
            if (x1 <= x2) return 0;
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
    
    public void addLink( final Node dest, final double bandwidth, final long delay, final int width, final int height, final String type )
    {
        float angle = calculateAngle( getCenterX(), getCenterY(), dest.getCenterX(), dest.getCenterY() );
        links.add( new Link( this, dest, bandwidth, delay, angle, width, height, type ) );
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
    			float angle = calculateAngle( node.getCenterX(), node.getCenterY(), linked.getDestNode().getCenterX(), linked.getDestNode().getCenterY() );
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
    	// TODO inserire la gestione dello spostamento di un nodo
        
    }
    
    public void draw( final Graphics2D g )
    {
        g.setColor( color );
        g.fillOval( node.x, node.y, node.width, node.height );
        
        g.setColor( Color.black );
        g.drawOval( node.x, node.y, node.width, node.height );
        
        g.setColor( Color.white );
        Rectangle2D bounds = g.getFontMetrics().getStringBounds( nodeID + "", g );
        g.drawString( nodeID + "",
                     (int) (node.getCenterX() - bounds.getWidth()/2),
                     (int) (node.getCenterY() + bounds.getHeight()/2) );
    }
}