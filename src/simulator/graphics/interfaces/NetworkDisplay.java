package simulator.graphics.interfaces;

import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.AnimationNetwork;
import simulator.graphics.elements.Event;
import simulator.graphics.elements.Info;
import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;
import simulator.topology.NetworkLink;

public class NetworkDisplay implements AnimationInterface
{
    private Rectangle zone;
    
    private List<Node> nodes;
    private List<Packet> packets;
    
    private final int width, height;
    
    private long timer = 0;
    
    private boolean start, pause;
    
    private int index = 0, indexElement = -1, packetSize;
    
    public static Info info;

	private int mouseX, mouseY;
	
	private Node tmpNode = null, source = null;

	private boolean moving = false, removing = false, addingPacket = false, addingNode = false;

    private boolean phaseTwoNewElement, phaseOneNewElement;
    
    private String CLIENT = "Client", SERVER = "Server", SWITCH = "Switch";
    
    public NetworkDisplay( int width, int height, float startY, List<Node> nodes, List<Packet> packets )
    {
    	this.width = width;
    	this.height = height;
        this.nodes = nodes;
        this.packets = packets;
    	
        zone = new Rectangle( 0, startY, width, height*100/142 );
        
        packetSize = packets.size();
        
        info = new Info();
        
        resetAnimation();
    }
    
    /***/
    private void resetAnimation() {
        timer = 0;
        index = 0;
        indexElement = -1;
        
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

    /**Adds a new node in the simulation
     * @throws SlickException */
    public void addNode( float mouseX, float mouseY, String type ) throws SlickException {
        if (!phaseOneNewElement && !phaseTwoNewElement) {
            phaseOneNewElement = true;
            addingNode = true;
            if (type.equals( CLIENT )) {
                tmpNode = new Node( mouseX, mouseY, 10, type, 0, Color.yellow, nodes.size() );
            } else if (type.equals( SERVER )) {
                tmpNode = new Node( mouseX, mouseY, 10, type, 0, Color.black, nodes.size() );
            } else if (type.equals( SWITCH )) {
                tmpNode = new Node( mouseX, mouseY, 10, type, 0, Color.blue, nodes.size() );
            }
            
            float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
            float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
            
            tmpNode.getArea().setLocation( x, y );
        }
    }
    
    public void addPacket( float mouseX, float mouseY ) {
        if (!phaseOneNewElement && !phaseTwoNewElement) {
            phaseOneNewElement = true;
            addingPacket = true;
        }
    }
    
    public void nodeInit() throws SlickException {
    	for (Node node: nodes) {
    		node.Init();
    	}
    }
    
    public float getMaxY() {
        return zone.getMaxY();
    }
    
    public long getTimeSimulation() {
        return timer;
    }
    
    public void setTimeSimulation( long val ) {
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
    
    public void removeNode() {
    	removing = !removing;
    	
    	for (Node node: nodes) {
    		node.setRemovable();
    	}
    }
    
    public boolean isRemoving() {
        return removing;
    }
    
    public void moveNode() {
    	moving = !moving;
    	
    	for (Node node: nodes) {
    		node.setSelectable();
    	}
    }
    
    public boolean isMoving() {
        return moving;
    }
    
    public boolean isAddingElement() {
        return addingNode || addingPacket;
    }
    
    private void manageRemoveNode( GameContainer gc ) {
    	for (Node node: nodes) {
    		node.removeLink( nodes.get( indexElement ) );
    		
    		for (int i = packets.size() - 1; i >= 0; i--) {
				Packet packet = packets.get( i );
				if (packet.getNodeSource().equals( nodes.get( indexElement ) ) || packet.getNodeDest().equals( node )) {
					packets.remove( packet );
				}
			}
			
			packetSize = packets.size();
    	}

		AnimationNetwork.timeSimulation = 0;
		for (Packet packet : packets) {
			if (packet.getEndTime() > AnimationNetwork.timeSimulation) {
				AnimationNetwork.timeSimulation = packet.getEndTime();
			}
		}
		
		nodes.get( indexElement ).removeLink( null );
		nodes.remove( nodes.get( indexElement ) );
		
		indexElement = -1;
    }
    
    private void manageAddElement( Event event, boolean mouseEvent ) {
        if (phaseOneNewElement) {
        	if (addingNode) {
        		if (mouseEvent && event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON ) && zone.contains( mouseX , mouseY )) {
        			phaseOneNewElement = false;
                    phaseTwoNewElement = true;
        		}
        		
        		float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
                float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
                
                tmpNode.getArea().setLocation( x, y );
        	} else if (addingPacket && indexElement != -1) {
            	source = nodes.get( indexElement );
            	source.setLinkAvailable();
                phaseOneNewElement = false;
                phaseTwoNewElement = true;
            }
        } else if (phaseTwoNewElement) {
            for (Node node: nodes) {
                if (event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON ) && node.checkCollision( mouseX, mouseY )) {
                    if (addingNode) {
                        nodes.add( tmpNode.clone( node, width, height ) );
                        node.addLink( nodes.get( nodes.size() - 1 ), 0, 0, width, height, NetworkLink.BIDIRECTIONAL );
                        
                        phaseTwoNewElement = false;
                        tmpNode = null;
                        
                        addingNode = false;
                        indexElement = -1;
                        
                        return;
                    } else if (addingPacket && !node.equals( source ) && node.checkLinks( source )) {
                    	Packet packet = new Packet( source, node, source.getColor(), 0, 100, width, height, 1 );
                        packets.add( packet );
                        packetSize++;
                        phaseTwoNewElement = false;
                        source.setLinkAvailable();
                        source = null;
                        
                        System.out.println( "PACCHETTO INSERITO" );
                        
                        // TODO CAPIRE PERCHE NON VIENE VISUALIZZATO IL NUOVO PACKET INSERITO
                        System.out.println( "STARTTIME = " + packet.getStartTime() );
                        
                        addingPacket = false;
                        indexElement = -1;
                        
                        return;
                    }
                }
            }
        }
    }
    
    public void manageMovingNode( GameContainer gc, Event event ) {
    	if (tmpNode == null) {
    		if (indexElement != -1) {
    			nodes.get( indexElement ).setMoving( true );
    			tmpNode = nodes.get( indexElement );
    		}
    	} else if (!event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
    		tmpNode.setMoving( false );
    		tmpNode = null;
    		indexElement = -1;
    	}
	}
    
    public void resetIndex() {
    	indexElement = -1;
    }
    
    public boolean checkClick( Event event, NetworkDisplay nd )  throws SlickException {
        if (indexElement == -1) {
        	for (Node node: nodes) {
        		if (node.checkCollision( mouseX, mouseY )) {
        			if (event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
                		if (moving || phaseOneNewElement || phaseTwoNewElement) {
                			indexElement = node.getIndex();
                		}
                	} else if (event.getInput().isMouseButtonDown( Input.MOUSE_RIGHT_BUTTON )) {
                		if (removing) {
                			indexElement = node.getIndex();
                		}
                	}
                	
        			return true;
        		}
        	}
        }
    	
    	return false;
    }
    
    public void update( int delta, GameContainer gc, AnimationManager am, Event event, NetworkDisplay nd, boolean mouseEvent )
    {
    	mouseX = gc.getInput().getMouseX();
    	mouseY = gc.getInput().getMouseY();
    	
        if (timer > AnimationNetwork.timeSimulation) {
            stopAnimation();
            am.resetAllButtons();
        } else if (!pause) {
            timer = timer + AnimationManager.frames;
        }
        
        // manage nodes and packets
        if (phaseOneNewElement || phaseTwoNewElement) {
            manageAddElement( event, mouseEvent );
        } else if (moving) {
        	manageMovingNode( gc, event );
        } else if (removing && indexElement != -1) {
        	manageRemoveNode( gc );
        }
    	
        for (Node node: nodes) {
            node.update( gc, width, zone.getY(), (int) zone.getMaxY(), (addingNode && phaseTwoNewElement) );
        }
        
        for (int i = index; i < packetSize; i++) {
        	//System.out.println( "INDEX = " + i );
            Packet packet = packets.get( i );
            if (packet.getStartTime() > timer)
                break;
            
            packet.update( gc, timer, start && !pause );
            if (!packet.isActive() && i == index) {
                index++;
            }
        }
    }
    
    public void render( GameContainer gc )
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
