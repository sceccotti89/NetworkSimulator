package simulator.graphics.interfaces;

import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

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
    
    public NetworkDisplay( final float width, final float height, final float startY, final List<Node> nodes, final List<Packet> packets, long timeSimulation )
    {
        zone = new Rectangle( 0, startY, width, height );
        
        this.nodes = nodes;
        
        this.packets = packets;
        
        this.timeSimulation = timeSimulation;
        
        packetSize = packets.size();
        
        resetAnimation();
    }
    
    /***/
    private void resetAnimation() {
        timer = 0;
        index = 0;
        
        start = false;
        pause = false;

        for (Packet packet: packets) {
            packet.init();
        }
    }
    
    public void checkActivityPackets() {
        index = 0;
        for (int i = index; i < packetSize; i++) {
            Packet packet = packets.get( i );
            if (timer >= packet.getStartTime()) {
                if (timer < packet.getEndTime()) {
                    packet.setPosition( timer );
                }
            } else if (i == index) {
                index++;
            }
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
    }
    
    public void setPacketSpeed( final int frames ) {
        for (Packet packet: packets) {
            packet.setSpeed( frames );
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
    
    public void update( final GameContainer gc, final AnimationManager am )
    {
        if (!start || pause) {
            return;
        }
        
        timer = timer + am.getFrames();
        
        for (int i = index; i < packetSize; i++) {
            Packet packet = packets.get( i );
            if (packet.getStartTime() > timer) break;
            packet.update( gc, timer );
            if (!packet.isActive() && i == index) {
                index++;
            }
        }
        
        if (timer >= timeSimulation) {
            stopAnimation();
            am.resetAllButtons();
        }
        
        for (Node node: nodes) {
            node.update( gc );
        }
    }
    
    public void render( final GameContainer gc ) {
        Graphics g = gc.getGraphics();
        
        g.setColor( Color.white );
        g.fill( zone );
        
        if (start) {
            for (int i = index; i < packetSize; i++) {
                Packet packet = packets.get( i );
                packet.render( timer, g );
            }
        }
        
        // TODO PROVVISORIO, POI FARO' COME HA DETTO STEFANO
        for (Node node: nodes) {
            node.drawLinks( g );
        }
        
        for (Node node: nodes) {
            node.drawNode( g );
        }
        
        /*for (Node node: nodes) {
            node.drawInfo( g );
        }*/
    }
}