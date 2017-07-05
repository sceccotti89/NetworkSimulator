package simulator.graphics.elements;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Circle;

import simulator.topology.NetworkLink;

public class Node
{
    private long nodeID;
    private Color color;
    
    private Circle node;
    final private float ray = 25;
    
    final private long delay;
    final private String name;
    
    private List<Link> links;
	private int mouseX, mouseY;
	
	private boolean selectable;
	
	private Image circleDashed;
	
	private int angle = 0;
    
    public Node( final float x, final float y, final long nodeID, final String name, final long delay, final Color color ) throws SlickException
    {
        this.nodeID = nodeID;
        this.color = color;
        
        this.delay = delay;
        this.name = name;
        
        node = new Circle( x, y, ray );
        
        links = new ArrayList<>();
    }
    
    public void Init() throws SlickException {
    	circleDashed = new Image( "./data/Image/Circle.png" );
        circleDashed.setCenterOfRotation( node.getCenterX(), node.getCenterY() );
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
    
    public Circle getArea(){
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
    
    private float angleValutation( final float x1, final float y1, final float x2, final float y2 )
    {
        float m = (y2 - y1)/(x2 - x1);
        
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
    
    public void addLink( final Node dest, final float x1, final float y1, final float x2, final float y2, final int width, final int height, final String type ) {
        links.add( new Link( this, dest, x1, y1, x2, y2, calculateAngle( x1, y1, x2, y2 ), width, height, type ) );
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
    
    public void setSelectable() {
    	selectable = !selectable;
    }
    
    public void setLinkPosition( Link link, Node source, Node dest ) {
    	for(Link linked: links) {
    		if (linked.getDestNode() == source && link.getDestNode() == dest) {
    			float angle = angleValutation( node.getCenterX(), node.getCenterY(), linked.getDestNode().getCenterX(), linked.getDestNode().getCenterY() );
        		linked.setPosition( this, angle );
    		}
    	}
    }
    
    public void update( final GameContainer gc ) {
    	mouseX = gc.getInput().getMouseX();
    	mouseY = gc.getInput().getMouseY();
    	
        for (Link link: links) {
            link.update( gc, mouseX, mouseY );
        }
        
        if (selectable) {
            circleDashed.setRotation( ++angle % 360 );
            
        	if (gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
                if (node.contains( mouseX, mouseY )) {
                	for(Link link: links) {
                		if (link.getType().equals( NetworkLink.BIDIRECTIONAL )) {
                			link.getDestNode().setLinkPosition( link, this, link.getDestNode() );
                		}
                		
                		float angle = angleValutation( node.getCenterX(), node.getCenterY(), link.getDestNode().getCenterX(), link.getDestNode().getCenterY() );
                		link.setPosition( this, angle );
                	}
                	node.setLocation( mouseX - ray, mouseY - ray );
                }
        	}
        }
    }
    
    public void drawLinks( final Graphics g ) {
        for (Link link: links) {
            link.render( g );
        }
    }
    
    public void drawNode( final Graphics g ) {
        g.setColor( color );
        g.fill( node );
        
        g.setColor( Color.black );
        g.draw( node );
        
        Font f = g.getFont();
        g.setColor( Color.white );
        g.drawString( nodeID + "", node.getCenterX() - f.getWidth( nodeID + "" )/2, node.getCenterY() - f.getHeight( nodeID + "" )/2 );
        
        if (selectable && circleDashed != null) {
        	g.drawImage( circleDashed, node.getX(), node.getY() );
        }
    }
}