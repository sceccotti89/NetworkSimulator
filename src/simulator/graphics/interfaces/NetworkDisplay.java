package simulator.graphics.interfaces;

import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.AnimationNetwork;
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
    
    private long timer = 0;
    
    private boolean start, pause;
    
    private int index, packetSize;
    
    public static Info info;
    
    private boolean nodesChanged = false;

	private int mouseX, mouseY;
	
	private Node nodeMoved = null;

	private Node tmpNode;

	private boolean removing, addingPacket, addingNode;
	
	private Node source = null;

    private boolean phaseTwoNewElement;

    private boolean phaseOneNewElement;
    
    public NetworkDisplay( final int width, final int height, final float startY, final List<Node> nodes, final List<Packet> packets )
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
    
    /**Add a new client node in the simulation
     * @throws SlickException */
    // TODO CREARE UN METODO UNICO PER TUTTE E 3 LE TIPOLOGIE
    public void addClient( final float mouseX, final float mouseY ) throws SlickException {
        if (!phaseOneNewElement && !phaseTwoNewElement) {
            phaseOneNewElement = true;
            tmpNode = new Node( mouseX, mouseY, 10, "client", 0, Color.yellow );

            float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
            float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
            
            tmpNode.getArea().setLocation( x, y );
        }
    }
    
    /**Add a new server node in the simulation
     * @throws SlickException */
    public void addServer( final float mouseX, final float mouseY ) throws SlickException {
        if (!phaseOneNewElement && !phaseTwoNewElement) {
            phaseOneNewElement = true;
            tmpNode = new Node( mouseX, mouseY, 10, "server", 0, Color.black );

            float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
            float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
            
            tmpNode.getArea().setLocation( x, y );
        }
    }
    
    /**Add a new switch node in the simulation
     * @throws SlickException */
    public void addSwitch( final float mouseX, final float mouseY ) throws SlickException {
        if (!phaseOneNewElement && !phaseTwoNewElement) {
            phaseOneNewElement = true;
            tmpNode = new Node( mouseX, mouseY, 10, "switch", 0, Color.blue );

            float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
            float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
            
            tmpNode.getArea().setLocation( x, y );
        }
    }
    
    public void addPacket( final float mouseX, final float mouseY ) {
        if (!phaseOneNewElement && !phaseTwoNewElement) {
            phaseOneNewElement = true;
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
    
    public boolean isMoving() {
        return nodesChanged;
    }
    
    public float getMaxY() {
        return zone.getMaxY();
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
    
    public void removeNode() {
    	removing = !removing;
    }
    
    public boolean isRemoving() {
        return removing;
    }
    
    private void manageRemoveNode( final GameContainer gc ) {
    	if (gc.getInput().isMousePressed( Input.MOUSE_RIGHT_BUTTON )) {
    		for (Node node: nodes) {
    			if (node.checkCollision( mouseX, mouseY )) {
    				for (Node nodo: nodes) {
    					nodo.removeLink( node );
    				}
    				
    				for (int i = packets.size() - 1; i >= 0; i--) {
    					Packet packet = packets.get( i );
    					if (packet.getNodeSource().equals( node ) || packet.getNodeDest().equals( node )) {
    						packets.remove( packet );
    					}
    				}
    				
    				packetSize = packets.size();
    				
    				AnimationNetwork.timeSimulation = 0;
    				for (Packet packet : packets) {
    					if (packet.getEndTime() > AnimationNetwork.timeSimulation) {
    						AnimationNetwork.timeSimulation = packet.getEndTime();
    					}
    				}
    				
    				node.removeLink( null );
    				nodes.remove( node );
    				removing = false;
    				
    				return;
    			}
    		}
    	}
    }
    
    private void manageAddElement( final boolean leftMouse ) {
        if (phaseOneNewElement) {
            if (leftMouse){
                if (addingNode) {
                    phaseOneNewElement = false;
                    phaseTwoNewElement = true;
                } else if (addingPacket) {
                    for (Node node: nodes) {
                        if (node.checkCollision( mouseX, mouseY )) {
                            source = node;
                            source.setLinkAvailable();
                            phaseOneNewElement = false;
                            phaseTwoNewElement = true;
                            break;
                        }
                    }
                }
            } else if (addingNode) {
                float x = Math.max( Math.min( mouseX - tmpNode.getRay(), width - tmpNode.getRay()*2 ), 0 );
                float y = Math.max( Math.min( mouseY - tmpNode.getRay(), zone.getMaxY() - tmpNode.getRay()*2 ), zone.getY() );
                
                tmpNode.getArea().setLocation( x, y );
            }
        } else if (phaseTwoNewElement) {
            for (Node node: nodes) {
                if (leftMouse && node.checkCollision( mouseX, mouseY )) {
                    if (addingNode) {
                        nodes.add( tmpNode.clone( node, width, height ) );
                        node.addLink( nodes.get( nodes.size() - 1 ), 0, 0, width, height, NetworkLink.BIDIRECTIONAL );
                        
                        phaseTwoNewElement = false;
                        tmpNode = null;
                        
                        addingNode = false;
                        
                        break;
                    } else if (addingPacket && !node.equals( source ) && node.checkLinks( source )) {
                        packets.add( new Packet( source, node, source.getColor(), 0, 100, width, height, 0 ) );
                        phaseTwoNewElement = false;
                        source.setLinkAvailable();
                        source = null;
                        
                        System.out.println( "PACCHETTO INSERITO" );
                        
                        addingPacket = false;
                        
                        break;
                    }
                }
            }
        }
    }
    
    public void setAddingNode() {
        addingNode = true;
    }
    
    public void setAddingPacket() {
        addingPacket = true;
    }
    
    public void manageMovingNode( final GameContainer gc ) {
    	if (gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
    		if (nodeMoved == null) {
	            for (Node node: nodes) {
	            	if (node.checkCollision( mouseX, mouseY )) {
	            		node.setMoving( true );
	            		nodeMoved = node;
	            	}
	            }
    		}
		} else if (nodeMoved != null) {
			nodeMoved.setMoving( false );
			nodeMoved = null;
		}
    }
    
    public void update( final GameContainer gc, final AnimationManager am, final boolean leftMouse ) throws SlickException
    {
    	mouseX = gc.getInput().getMouseX();
    	mouseY = gc.getInput().getMouseY();
    	
        if (timer > AnimationNetwork.timeSimulation) {
            stopAnimation();
            am.resetAllButtons();
        } else if (!pause) {
            timer = timer + AnimationManager.frames;
        }
        
        // Reset the visibility at the very beginning.
        info.setVisible( false );
        
        if (phaseOneNewElement || phaseTwoNewElement) {
            manageAddElement( leftMouse );
        }
        
        if (nodesChanged) {
        	manageMovingNode( gc );
        }
        
        if (removing) {
        	manageRemoveNode( gc );
        }
    	
        for (Node node: nodes) {
            node.update( gc, width, zone.getY(), (int) zone.getMaxY(), phaseTwoNewElement );
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