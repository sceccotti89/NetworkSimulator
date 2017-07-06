package simulator.graphics.interfaces;

import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.elements.Info;
import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;

public class NetworkDisplay
{
    private Rectangle zone;
    
    private List<Node> nodes;
    private List<Packet> packets;
    
    private long timer = 0, timeSimulation;
    
    private boolean start, pause;
    
    private int index, packetSize;
    
    public static Info info;
    
    private boolean nodesChanged = false;
    
    public NetworkDisplay( final float width, final float height, final float startY, final List<Node> nodes, final List<Packet> packets, final long timeSimulation )
    {
        zone = new Rectangle( 0, startY, width, height );
        
        this.nodes = nodes;
        
        this.packets = packets;
        
        this.timeSimulation = timeSimulation;
        
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
            packet.init();
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
    
    public void nodeInit() throws SlickException {
    	for (Node node: nodes) {
    		node.Init();
    	}
    }
    
    // TODO IMPOSTARE CHE I NODI POSSANO ESSERE SELEZIONATI E SPOSTATI
    // SOLO PRIMA DI INIZIARE UNA SIMULAZIONE
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
            packet.setSpeed( AnimationManager.frames );
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
        if (timer > timeSimulation) {
            stopAnimation();
            am.resetAllButtons();
        } else if (!pause) {
            timer = timer + AnimationManager.frames;
        }
        
        // Reset the visibility at the very beginning.
        info.setVisible( false );
        
        for (Node node: nodes) {
            node.update( gc );
        }
        
        for (int i = index; i < packetSize; i++) {
            Packet packet = packets.get( i );
            if (packet.getStartTime() > timer)
                break;
            
            if (nodesChanged) {
            	packet.setPosition( packet.getNodeSource() );
            }
            
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
        
        info.render( g );
    }
}