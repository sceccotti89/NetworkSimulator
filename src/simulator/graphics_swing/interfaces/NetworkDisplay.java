
package simulator.graphics_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollBar;

import simulator.graphics_swing.AnimationNetwork;
import simulator.graphics_swing.elements.Info;
import simulator.graphics_swing.elements.Link;
import simulator.graphics_swing.elements.Node;
import simulator.graphics_swing.elements.Packet;

public class NetworkDisplay extends JPanel implements MouseMotionListener, ComponentListener
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 6828820513635876566L;
    
    private AnimationManager am;
    
    private int index;
    private int packetSize;
    private long timer = 0;
    
    private JScrollBar scrolls;
    
    public static Info info;
    
    private List<Node> nodes;
    private List<Link> links;
    private List<Packet> packets;
    
    private boolean start;
    private boolean pause;
    
    private Point mouse;

    public NetworkDisplay( final AnimationManager am, final float width, final float height )
    {
        addMouseMotionListener( this );
        addComponentListener( this );
        setDoubleBuffered( true );
        
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        
        this.am = am;
        am.setNetworkDisplay( this );
        
        mouse = new Point( -1, -1 );
        info = new Info();
        
        addComponentsToPane();
    }
    
    private void addComponentsToPane()
    {
        scrolls = new JScrollBar( JScrollBar.VERTICAL );
        //scrolls.setVisible( false );
        add( scrolls );
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
        if (timer > AnimationNetwork.timeSimulation) {
            stopAnimation();
            am.resetButtons();
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
    }

    @Override
    public void componentResized( final ComponentEvent e ) {
        remove( scrolls );
        addComponentsToPane();
    }

    @Override
    public void componentMoved(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void componentShown(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void componentHidden(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }
}