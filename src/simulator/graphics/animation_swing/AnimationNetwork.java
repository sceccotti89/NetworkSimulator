/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animation_swing;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import org.json.JSONArray;
import org.json.JSONObject;

import simulator.graphics.animation_swing.elements.Link;
import simulator.graphics.animation_swing.elements.Node;
import simulator.graphics.animation_swing.elements.Packet;
import simulator.graphics.animation_swing.interfaces.AnimationManager;
import simulator.graphics.animation_swing.interfaces.MenuAnimationBar;
import simulator.graphics.animation_swing.interfaces.NetworkDisplay;
import simulator.topology.NetworkLink;
import simulator.topology.NetworkNode;

public class AnimationNetwork extends WindowAdapter implements ComponentListener
{
    private JFrame frame;
    private Timer timer;
    
    private NetworkDisplay nd;
    private AnimationManager am;
    private JScrollPane scroll;
    
    private final int height;
    private final int width;
    
    private List<Node> nodes;
    private List<Link> links;
    private List<Packet> packets;
    
    private long timeSimulation = 0;
	private final int limit = 1000000;
	
	public static int fps = 30;
	
	
    
    public AnimationNetwork( int width, int height, String title )
    {
        this.height = height;
        this.width = width;
        
        nodes   = new ArrayList<>();
        links   = new ArrayList<>();
        packets = new ArrayList<>();
        
        createAndShowGUI( title, width, height );
        setTimer( 30 );
    }
    
    private void createAndShowGUI( String title, int width, int height )
    {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width  - width)/2;
        int y = (dim.height - height)/2;
        
        frame = new JFrame( title );
        frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
        
        MenuAnimationBar menuBar = new MenuAnimationBar();
        frame.setJMenuBar( menuBar );
        
        frame.setLocationRelativeTo( null );
        frame.setBounds( x, y, width, height );
        frame.setMinimumSize( new Dimension( 500, 300 ) );
        
        addComponentsToPane( frame.getContentPane(), width, height );
        
        frame.pack();
        frame.setVisible( false );
        
        frame.addWindowListener( this );
        frame.addComponentListener( this );
    }
    
    private void addComponentsToPane( Container pane, float width, float height )
    {
        pane.setLayout( new BoxLayout( pane, BoxLayout.Y_AXIS ) );
        //pane.setLayout( new BorderLayout() );
        pane.add( scroll = new JScrollPane( nd = new NetworkDisplay( width, height * 0.8f ) ) );
        pane.add( am = new AnimationManager( nd, width, height * 0.2f ) );
    }
    
    /**
     * Extrapolates infos from file to create packets.
     * 
     * @param file    file to read
    */
    public void loadPackets( String file ) throws IOException
    {
        System.out.println( "Loading from " + file + "..." );
        
        BufferedReader reader = new BufferedReader( new FileReader( file ) );
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] words = line.split( "[\\s|\\t]+" );
            
            final long from_ID = Long.parseLong( words[0] );
            final long startTime = Long.parseLong( words[1] );
            final Node from = getNode( from_ID );
            final long dest_ID = Long.parseLong( words[2] );
            final Color color = from.getColor();
            final long endTime = Long.parseLong( words[3] );
            final int type = Integer.parseInt( words[4] );
            
            addPacket( from_ID, dest_ID, color, startTime, endTime, type );
            
            if (endTime > timeSimulation) {
            	timeSimulation = endTime;
            }
        }
        
        reader.close();
        
        Collections.sort( packets );
        
        String measure = "MICRO";
        if (timeSimulation >= limit) {
        	measure = "TIME";
        }
        for (Packet packet: packets) {
        	packet.setMeasure( measure );
        }
        
        am.setAnimationTime( timeSimulation );
        
        System.out.println( "Loading completed." );
    }
    
    public void loadNetwork( String filename ) throws IOException
    {
        /** File structure:
         * 
         * nodes => [{id, name, delay, [xPos, yPos]}]
         * links => [{fromId, destId, bw, delay, [linkType]}]
        */
        
        BufferedReader br = new BufferedReader( new FileReader( filename ) );
        StringBuilder content = new StringBuilder( 512 );
        
        /**
         * File structure:
         * 
         * networks => [{[nodes],[links]}, ...]
        */
        
        String nextLine = null;
        while((nextLine = br.readLine()) != null)
            content.append( nextLine.trim() );
        JSONObject settings = new JSONObject( content.toString() );
        
        // Get the list of nodes.
        JSONArray nodes = settings.getJSONArray( "nodes" );
        int length = nodes.length();
        for(int i = 0; i < length; i++) {
            JSONObject node = nodes.getJSONObject( i );
            long id     = node.getLong( NetworkNode.ID );
            String name = node.getString( NetworkNode.NAME );
            long delay  = node.getLong( NetworkNode.DELAY );
            int xPos    = (node.has( NetworkNode.X_POS )) ? node.getInt( NetworkNode.X_POS ) : 0;
            int yPos    = (node.has( NetworkNode.Y_POS )) ? node.getInt( NetworkNode.Y_POS ) : 0;
            Color color = (node.has( "color" )) ? Color.decode( node.getString( "color" ) ) : Color.BLACK;
            
            addNode( xPos, yPos, id, name, delay, color );
        }
        
        // Get the list of links.
        JSONArray links = settings.getJSONArray( "links" );
        length = links.length();
        for(int i = 0; i < length; i++) {
            JSONObject link  = links.getJSONObject( i );
            long fromId      = link.getLong( NetworkLink.FROM_ID );
            long destId      = link.getLong( NetworkLink.DEST_ID );
            double bandwidth = link.getDouble( NetworkLink.BANDWIDTH );
            long delay       = link.getLong( NetworkLink.DELAY );
            String linkType  = (link.has( NetworkLink.LINK_TYPE )) ?
                                 link.getString( NetworkLink.LINK_TYPE ) :
                                 NetworkLink.UNIDIRECTIONAL;
            
            addLink( fromId, destId, bandwidth, delay, linkType );
        }
        
        br.close();
    }
    
    public void addNode( int x, int y, long nodeID, String name, long delay, Color color ) {
        Node node = new Node( x, y, nodeID, name, delay, color );
        nodes.add( node );
    }
    
    public void addPacket( long from_ID, long dest_ID, Color color, long startTime, long endTime, int type ) {
        Link link = getLink( from_ID, dest_ID );
        Packet packet = new Packet( getNode( from_ID ), getNode( dest_ID ), link,
                                    color, startTime, endTime, width, height, type );
        packet.setSpeed();
        packets.add( packet );
    }
    
    public void addLink( long source, long dest, double bandwidth, long delay, String type )
    {
        Node source_node = getNode( source );
        Node dest_node   = getNode( dest );
        float angle = source_node.calculateAngle( dest_node );
        links.add( new Link( source_node, dest_node, bandwidth, delay, angle, width, height, type ) );
    }
    
    private Node getNode( long nodeID )
    {
        for (Node node: nodes) {
            if (node.getID() == nodeID) {
                return node;
            }
        }
        
        return null;
    }
    
    private Link getLink( long sourceID, long destID )
    {
        for (Link link: links) {
            if (link.getSourceNode().getID() == sourceID &&
                link.getDestNode().getID() == destID ||
                link.getSourceNode().getID() == destID &&
                link.getDestNode().getID() == sourceID) {
                return link;
            }
        }
        
        return null;
    }
    
    public void loadSimulation( String networkFile, String packetFile ) throws IOException
    {
        loadNetwork( networkFile );
        loadPackets( packetFile );
    }
    
    public void start()
    {
        nd.setElements( nodes, links, packets );
        frame.setVisible( true );
        frame.requestFocusInWindow();
        timer.restart();
    }
    
    /**
     * Set the timer of the animation.</br>
     * By default this animator makes use of a timer of 30 FPS,
     * just for the animation repainting.
     * 
     * @param FPS    frame per second
    */
    public void setTimer( int FPS )
    {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                am.update();
                nd.repaint();
            }
        };
        setTimer( FPS, listener );
    }
    
    /**
     * Set the timer of the animation with a specific action listener.</br>
     * By default the FPS are fixed to 30, just for the animation repainting.
     * 
     * @param FPS         frame per second
     * @param listener    the associated event listener
    */
    public void setTimer( int FPS, ActionListener listener ) {
        fps = FPS;
        timer = new Timer( 1000 / FPS, listener );
    }
    
    @Override
    public void windowClosing( WindowEvent e )
    {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            dispose();
        }
    }
    
    private void dispose() {
        frame.dispose();
        timer.stop();
    }

    @Override
    public void componentHidden( ComponentEvent e ) {}

    @Override
    public void componentMoved( ComponentEvent e ) {}

    @Override
    public void componentResized( ComponentEvent e )
    {
        //System.out.println( "SIZE: " + frame.getSize() );
        scroll.setPreferredSize( new Dimension( frame.getWidth(), (int) (frame.getHeight() * 0.8) ) );
        nd.setSize( new Dimension( frame.getWidth(), (int) (frame.getHeight() * 0.8) ) );
        am.setPreferredSize( new Dimension( frame.getWidth(), (int) (frame.getHeight() * 0.2) ) );
    }

    @Override
    public void componentShown( ComponentEvent e ) {}
}
