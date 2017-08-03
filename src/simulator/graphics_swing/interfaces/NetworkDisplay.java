
package simulator.graphics_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import simulator.graphics_swing.AnimationNetwork;
import simulator.graphics_swing.elements.Info;
import simulator.graphics_swing.elements.Link;
import simulator.graphics_swing.elements.Node;
import simulator.graphics_swing.elements.Packet;

public class NetworkDisplay extends JPanel
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 6828820513635876566L;
    
    private AnimationManager am;
    
    private int index;
    private int packetSize;
    private long timer = 0;
    
    public static Info info;
    
    private List<Node> nodes;
    private List<Link> links;
    private List<Packet> packets;
    
    private boolean start;
    private boolean pause;
    
    

    public NetworkDisplay( final AnimationManager am, final float width, final float height )
    {
        this.am = am;
        am.setNetworkDisplay( this );
        
        info = new Info();
        
        setDoubleBuffered( true );
        setPreferredSize( new Dimension( (int) width, (int) height ) );
    }
    
    public void setElements( final List<Node> nodes, final List<Link> links, final List<Packet> packets )
    {
        this.nodes = nodes;
        this.links = links;
        this.packets = packets;
        packetSize = packets.size();
        resetAnimation();
    }
    
    private void resetAnimation()
    {
        setTime( 0 );
        index = 0;
        
        start = false;
        pause = true;

        for (Packet packet: packets) {
            packet.init( packet.getStartTime() );
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
        
        for (Node node: nodes) {
            //node.update( width, zone.getY(), (int) zone.getMaxY(), (addingNode && phaseTwoNewElement) );
            node.update();
        }
        
        /*for (Packet packet: packets) {
            packet.setPosition( timer );
        }*/
        
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
        setBackground( Color.LIGHT_GRAY );
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON );
        
        for (int i = index; i < packetSize; i++) {
            packets.get( i ).draw( g2, timer );
        }
        
        for (Link link: links) {
            link.draw( g2 );
        }
        
        for (Node node: nodes) {
            node.draw( g2 );
        }
    }
}