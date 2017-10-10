/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animation_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.JPanel;

import simulator.graphics.animation_swing.AnimationNetwork;
import simulator.graphics.animation_swing.elements.Info;
import simulator.graphics.animation_swing.elements.Link;
import simulator.graphics.animation_swing.elements.Node;
import simulator.graphics.animation_swing.elements.Packet;

public class NetworkDisplay extends JPanel implements MouseMotionListener
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
    
    private long timeSimulation = 0;
    
    private boolean start;
    private boolean pause;
    
    private Point mouse;
    
    private static final int SCROLL_WIDTH = 15;
    
    

    public NetworkDisplay( final float width, final float height )
    {
        addMouseMotionListener( this );
        setDoubleBuffered( true );
        
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        
        mouse = new Point( -1, -1 );
        info = new Info();
    }
    
    public void setAnimationManager( final AnimationManager am ) {
        this.am = am;
    }
    
    public void setElements( final List<Node> nodes, final List<Link> links, final List<Packet> packets )
    {
        this.nodes = nodes;
        this.links = links;
        this.packets = packets;
        packetSize = packets.size();
        resetAnimation();
        
        for (Packet pkt : packets) {
            if (pkt.getEndTime() > timeSimulation) {
                timeSimulation = pkt.getEndTime();
            }
        }
        
        // Get the panel dimension.
        Dimension size = getPreferredSize();
        double maxX = 0, maxY = 0;
        for (Node node : nodes) {
            maxX = Math.max( maxX, node.getArea().getMaxX() );
            maxY = Math.max( maxY, node.getArea().getMaxY() + 1 );
        }
        
        if (maxX > size.getWidth() && maxY > size.getHeight()) {
            setPreferredSize( new Dimension( (int) (maxX - SCROLL_WIDTH), (int) (maxY - SCROLL_WIDTH) ) );
        } else {
            if (maxY > size.getHeight()) {
                setPreferredSize( new Dimension( (int) (maxX - SCROLL_WIDTH), (int) maxY ) );
            } else {
                if (maxX > size.getWidth()) {
                    setPreferredSize( new Dimension( (int) maxX, (int) (maxY - SCROLL_WIDTH) ) );
                }
            }
        }
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
    
    public boolean isPauseAnimation() {
        return pause;
    }
    
    public void stopAnimation() {
        resetAnimation();
    }
    
    public long getTime() {
        return timer;
    }
    
    public void setTime( final long time )
    {
        timer = time;
        for (Packet packet : packets) {
            packet.setPosition( timer );
        }
        repaint();
    }
    
    private void update( final Graphics2D g, final int delta )
    {
        if (timer > timeSimulation) {
            stopAnimation();
            am.reset();
        } else if (!pause) {
            timer = timer + AnimationManager.frames;
        }
        
        info.setVisible( false );
        
        for (Link link : links) {
            link.update( g, mouse );
        }
        
        for (Node node: nodes) {
            //node.update( width, zone.getY(), (int) zone.getMaxY(), (addingNode && phaseTwoNewElement) );
            node.update();
        }
        
        for (int i = index; i < packetSize; i++) {
            Packet packet = packets.get( i );
            if (packet.getStartTime() > timer)
                break;
            
            packet.update( g, timer, start && !pause, mouse );
            if (!packet.isActive() && i == index) {
                index++;
            }
        }
    }
    
    @Override
    public void mouseDragged( final MouseEvent event ) {}

    @Override
    public void mouseMoved( final MouseEvent event ) {
        mouse = event.getPoint();
    }

    @Override
    protected void paintComponent( final Graphics g )
    {
        super.paintComponent( g );
        setBackground( Color.LIGHT_GRAY );
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON );
        
        this.update( g2, AnimationNetwork.fps );
        
        for (int i = index; i < packetSize; i++) {
            packets.get( i ).draw( g2, timer );
        }
        
        for (Link link: links) {
            link.draw( g2 );
        }
        
        for (Node node: nodes) {
            node.draw( g2 );
        }
        
        info.render( g2 );
        
        g2.dispose();
    }
}