package simulator.graphics.elements;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
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
	private int mouseX = 0, mouseY = 0;
	private int moveX = 0, moveY = 0;
	
	private boolean selectable, moving, choose;
	
	private Image whiteCircle, redCircle, greenCircle;
	
	private int angle = 0;
	private boolean removable;
	
	private int index;
    
    public Node( float x, float y, long nodeID, String name, long delay, Color color, int index )
    {
        this.nodeID = nodeID;
        this.color = color;
        
        this.delay = delay;
        this.name = name;
        
        node = new Circle( x, y, ray );
        
        links = new ArrayList<>();
        
        this.index = index;
    }
    
    public void Init() throws SlickException {
    	whiteCircle = new Image( "./data/Image/Circle.png" );
        whiteCircle.setCenterOfRotation( node.getCenterX(), node.getCenterY() );
        
        redCircle = new Image( "./data/Image/RedCircle.png" );
        redCircle.setCenterOfRotation( node.getCenterX(), node.getCenterY() );
        
        greenCircle = new Image( "./data/Image/GreenCircle.png" );
        greenCircle.setCenterOfRotation( node.getCenterX(), node.getCenterY() );
    }
    
    public int getIndex() {
    	return index;
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
    
    public Float getAngle( long destID ) {
        for (Link link: links) {
            if (link.getDestNode().getNodeID() == destID)
                return link.getAngle();
        }
        
        return null;
    }
    
    public int numberLinks() {
        return links.size();
    }
    
    private float angleValutation( float x1, float y1, float x2, float y2 )
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

    private float calculateAngle( float x1, float y1, float x2, float y2 ) {
        return angleValutation( x1, y1, x2, y2 );
    }
    
    public void addLink( Node dest, double bandwidth, long delay, int width, int height, String type ) {
        float angle = calculateAngle( getCenterX(), getCenterY(), dest.getCenterX(), dest.getCenterY() );
        links.add( new Link( this, dest, bandwidth, delay, angle, width, height, type ) );
    }
    
    public Float getLinkLenght( long destID ) {
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
    
    public void setLinkPosition( Link link, Node source ) {
    	for(Link linked: links) {
    		if (linked.getDestNode() == source) {
    			float angle = angleValutation( node.getCenterX(), node.getCenterY(), linked.getDestNode().getCenterX(), linked.getDestNode().getCenterY() );
        		linked.setPosition( this, angle );
    		}
    	}
    }
    
    public boolean isMoving() {
    	return moving;
    }
    
    public void setMoving( boolean val ) {
    	moving = val;
    }
    
    public boolean checkCollision( int mouseX, int mouseY ) {
    	return node.contains( mouseX, mouseY );
    }
    
    public boolean checkLinks( Node source ) {
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
    
    public Node clone( Node dest, int width, int height ) {
    	try {
			Node tmp = new Node( node.getCenterX(), node.getCenterY(), nodeID, name, delay, color, index );
			tmp.addLink( dest, 0, 0, width, height, NetworkLink.BIDIRECTIONAL );
			tmp.Init();
			return tmp;
		} catch (SlickException e1) {
			e1.printStackTrace();
		}
    	
    	return null;
    }
    
    public void update( GameContainer gc, int widthSpace, float startSpaceY, int heightSpace, boolean choose ) {
    	this.choose = choose;
    	
    	moveX = mouseX; moveY = mouseY;
    	mouseX = gc.getInput().getMouseX();
    	mouseY = gc.getInput().getMouseY();
    	
        for (Link link: links) {
            link.update( gc, mouseX, mouseY );
        }
        
        if (choose) {
        	greenCircle.setRotation( ++angle );
        } else if (removable) {
        	redCircle.setRotation( ++angle );
        } else if (selectable) {
            whiteCircle.setRotation( ++angle );
            
            if (moving) {
            	float x = Math.max( Math.min( getCenterX() - ray + mouseX - moveX, widthSpace - ray*2 ), 0 );
            	float y = Math.max( Math.min( getCenterY() - ray + mouseY - moveY, heightSpace - ray*2 ), startSpaceY );
            	
            	node.setLocation( x, y );
            	
            	for(Link link: links) {
            		setLinkPosition( link, link.getDestNode() );
            		
            		if (link.getType().equals( NetworkLink.BIDIRECTIONAL )) {
            			link.getDestNode().setLinkPosition( link, this );
            		}
            	}
            }
        }
        
        moveX = mouseX;
        moveY = mouseY;
    }
    
    public void drawLinks( Graphics g ) {
        for (Link link: links) {
            link.render( g );
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
        
        if (selectable) {
        	g.drawImage( whiteCircle, node.getX(), node.getY() );
        }
        
        if (choose) {
        	g.drawImage( greenCircle, node.getX(), node.getY() );
        }
        
        if (removable) {
        	g.drawImage( redCircle, node.getX(), node.getY() );
        }
    }
}
