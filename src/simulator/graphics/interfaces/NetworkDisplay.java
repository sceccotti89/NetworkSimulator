package simulator.graphics.interfaces;

import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.elements.Info;
import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;
import simulator.topology.NetworkLink;

public class NetworkDisplay
{
    private Rectangle zone;
    
    private List<Node> nodes;
    private List<Packet> packets;
    
    private final int width, height;
    
    private long timer = 0, timeSimulation;
    
    private boolean start, pause;
    
    private int index, packetSize;
    
    public static Info info;
    
    private boolean nodesChanged = false;

	private int mouseX, mouseY;
	
	private Node nodeMoved = null;
	
	private boolean phaseOneNewNode = false, phaseTwoNewNode = false;

	private Node tmpNode;
    
    public NetworkDisplay( final int width, final int height, final float startY, final List<Node> nodes, final List<Packet> packets, final long timeSimulation )
    {
    	this.width = width;
    	this.height = height;
        this.nodes = nodes;
        this.packets = packets;
        this.timeSimulation = timeSimulation;
    	
        zone = new Rectangle( 0, startY, width, height*100/142 );
        
        packetSize = packets.size();
        
        info = new Info();
        
        resetAnimation();
    }
    
    /***/
    private void resetAnimation() {
        timer = 0;
        index = 0;
        
        start = false;
        pause = true;

        for (Packet packet: packets) {
            packet.init( packet.getNodeSource(), packet.getNodeDest(), packet.getStartTime() );
        }
    }
    
    public void checkActivityPackets() {
        index = 0;
        for (int i = index; i < packetSize; i++) {
            Packet packet = packets.get( i );
            packet.setPosition( timer );
            if (timer < packet.getStartTime() && i == index) {
                index++;
            }
        }
    }
    
    // TODO COMPLETARE QUESTO METODO
    /**add a new node in the simulation
     * @throws SlickException */
    public void addNewNode( final int mouseX, final int mouseY ) throws SlickException {
    	if (!phaseOneNewNode && !phaseTwoNewNode) {
    		phaseOneNewNode = true;
    		tmpNode = new Node( mouseX, mouseY, 1, "switch", 0, Color.gray );

    		float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
        	float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
        	
    		tmpNode.getArea().setLocation( x, y );
    	}
    }
    
    public void nodeInit() throws SlickException {
    	for (Node node: nodes) {
    		node.Init();
    	}
    }
    
    public void setNodeSelectable() {
    	nodesChanged = !nodesChanged;
    	
    	for (Node node: nodes) {
    		node.setSelectable();
    	}
    }
    
    public float getMaxY() {
        return zone.getMaxY();
    }
    
    public long getTotalTimeSimulation() {
    	return timeSimulation;
    }
    
    public long getTimeSimulation() {
        return timer;
    }
    
    public void setTimeSimulation( final long val ) {
        timer = val;
        checkActivityPackets();
    }
    
    public void setPacketSpeed() {
        for (Packet packet: packets) {
            packet.setSpeed();
        }
    }
    
    public boolean isInPause() {
        return pause;
    }
    
    public boolean isInExecution(){
    	return start;
    }
    
    public void startAnimation() {
    	start = true;
        pause = false;
    }
    
    public void pauseAnimation() {
        pause = true;
    }
    
    public void stopAnimation() {
        resetAnimation();
    }
    
    public void update( final GameContainer gc, final AnimationManager am ) throws SlickException
    {
    	mouseX = gc.getInput().getMouseX();
    	mouseY = gc.getInput().getMouseY();
    	
        if (timer > timeSimulation) {
            stopAnimation();
            am.resetAllButtons();
        } else if (!pause) {
            timer = timer + AnimationManager.frames;
        }
        
        // Reset the visibility at the very beginning.
        info.setVisible( false );
        
        // TODO TRASFORMARLO IN METODO
        if (phaseOneNewNode) {
        	if (gc.getInput().isMousePressed( Input.MOUSE_LEFT_BUTTON )) {
        		phaseOneNewNode = false;
        		phaseTwoNewNode = true;
        	} else {
        		float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
            	float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
            	
        		tmpNode.getArea().setLocation( x, y );
        	}
        } else if (phaseTwoNewNode) {
        	for (Node node: nodes) {
        		if (gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON ) && node.checkCollision( mouseX, mouseY )) {
        			if (nodesChanged) {
        				tmpNode.setSelectable();
        			}
        			tmpNode.addLink( node, 0, 0, width, height, NetworkLink.BIDIRECTIONAL );
        			node.addLink( tmpNode, 0, 0, width, height, NetworkLink.BIDIRECTIONAL );
        			tmpNode.Init();
        			nodes.add( tmpNode );
        			tmpNode = null;
        			
        			phaseTwoNewNode = false;
        			
        			break;
        		}
        	}
        	
        }
        
        // TODO TRASFORMARLO IN METODO
    	if (nodesChanged) {
    		if (gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
	    		if (nodeMoved == null) {
		            for (Node node: nodes) {
		            	if (node.checkCollision( mouseX, mouseY )) {
		            		node.setMoving( true );
		            		nodeMoved = node;
		            	}
		            }
	    		} else if (!nodeMoved.checkCollision( mouseX, mouseY )) {
	    			nodeMoved.setMoving( false );
	    			nodeMoved = null;
	    		}
    		} else if (nodeMoved != null) {
    			nodeMoved.setMoving( false ) ;
    		}
    	} else {
    		nodeMoved = null;
    	}
    	
        for (Node node: nodes) {
            node.update( gc, width, zone.getY(), (int) zone.getMaxY(), phaseTwoNewNode );
        }
        
        for (Packet packet: packets) {
            packet.init( packet.getNodeSource(), packet.getNodeDest(), timer );
        }
        
        for (int i = index; i < packetSize; i++) {
            Packet packet = packets.get( i );
            if (packet.getStartTime() > timer)
                break;
            
            packet.update( gc, timer, start && !pause );
            if (!packet.isActive() && i == index) {
                index++;
            }
        }
    }
    
    public void render( final GameContainer gc ) throws SlickException
    {
        Graphics g = gc.getGraphics();
        
        g.setColor( Color.white );
        g.fill( zone );
        
        for (int i = index; i < packetSize; i++) {
            packets.get( i ).render( g, timer );
        }
        
        // TODO PROVVISORIO, POI FARO' COME HA DETTO STEFANO
        for (Node node: nodes) {
            node.drawLinks( g );
        }
        
        for (Node node: nodes) {
            node.drawNode( g );
        }
        
        if (tmpNode != null) {
        	tmpNode.drawNode( g );
        }
        
        info.render( g );
    }
}