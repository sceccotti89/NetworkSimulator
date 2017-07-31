
package simulator.graphics_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

import simulator.graphics.AnimationNetwork;
import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;

public class NetworkDisplay extends JPanel
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 6828820513635876566L;
    
    private AnimationManager am;
    
    private int index;
    private long timer = 0;
    
    private List<Node> nodes;
    private List<Packet> packets;
    
    private boolean start;
    private boolean pause;

    public NetworkDisplay( final AnimationManager am, final float width, final float height )
    {
        this.am = am;
        am.setNetworkDisplay( this );
        
        setPreferredSize( new Dimension( (int) width, (int) height ) );
    
        resetAnimation();
    }
    
    public void setElements( final List<Node> nodes, final List<Packet> packets ) {
        this.nodes = nodes;
        this.packets = packets;
    }
    
    private void resetAnimation()
    {
        setTime( 0 );
        index = 0;
        
        start = false;
        pause = true;

        for (Packet packet: packets) {
            packet.init( packet.getNodeSource(), packet.getNodeDest(), packet.getStartTime() );
        }
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
    
    public void setTime( final long time )
    {
        timer = time;
        repaint();
    }
    
    public void update( final int delta )
    {
        if (timer > AnimationNetwork.timeSimulation) {
            stopAnimation();
            am.resetButtons();
        } else if (!pause) {
            timer = timer + AnimationManager.frames;
        }
        
        timer = timer + delta;
        
        for (Node node: nodes) {
            node.update( width, zone.getY(), (int) zone.getMaxY(), (addingNode && phaseTwoNewElement) );
        }
        
        for (Packet packet: packets) {
            packet.init( packet.getNodeSource(), packet.getNodeDest(), timer );
        }
        
        for (int i = index; i < packetSize; i++) {
            Packet packet = packets.get( i );
            if (packet.getStartTime() > timer)
                break;
            
            packet.update( timer, start && !pause );
            if (!packet.isActive() && i == index) {
                index++;
            }
        }
    }
    
    @Override
    protected void paintComponent( final Graphics g )
    {
        super.paintComponent( g );
        
        Graphics2D g2 = (Graphics2D) g;
        
        setBackground( Color.LIGHT_GRAY );
        
        for (Node node: nodes) {
            node.drawLinks( g2 );
        }
        
        for (Node node: nodes) {
            node.drawNode( g2 );
        }
    }
}