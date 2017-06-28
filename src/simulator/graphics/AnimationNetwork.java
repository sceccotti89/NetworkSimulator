
package simulator.graphics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;
import simulator.graphics.interfaces.AnimationManager;
import simulator.graphics.interfaces.NetworkDisplay;
import simulator.graphics.interfaces.OptionBar;
import simulator.graphics.interfaces.TimeAnimation;
import simulator.topology.NetworkLink;
import simulator.topology.NetworkNode;

public class AnimationNetwork extends BasicGame
{
    private OptionBar ob;
    private AnimationManager am;
    private TimeAnimation ta;
    private NetworkDisplay nd; 
    
    private boolean leftMouse;
    
    private List<Node> nodes;
    private List<Packet> packets;
    
    private int width, height;
    
    private long timeDuration = 0;
    
    public AnimationNetwork( final int width, final int height, final String title )
    {
        super( title );
        
        nodes   = new ArrayList<Node>();
        packets = new ArrayList<Packet>();
        
        this.width = width;
        this.height = height;
    }
    
    /**
     * Extrapolates infos from file to create packets.
     * 
     * @param file    file to read
    */
    public void loadPackets( final String file ) throws IOException
    {
        System.out.println( "Loading from " + file + "..." );
        
        BufferedReader reader = new BufferedReader( new FileReader( file ) );
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] words = line.split( "[\\s|\\t]+" );
            
            final long from_ID = Long.parseLong( words[0] );
            final long startTime = Long.parseLong( words[1] );
            Node from = getNode( from_ID );
            final long dest_ID = Long.parseLong( words[2] );
            final Color color = from.getColor();
            final long endTime = Long.parseLong( words[3] );
            final int type = Integer.parseInt( words[4] );
            
            addPacket( from_ID, dest_ID, color, startTime, endTime, type );
            
            if (endTime > timeDuration) {
                timeDuration = endTime;
            }
        }
        
        reader.close();
        
        Collections.sort( packets );
        
        System.out.println( "Loading completed." );
    }
    
    public void loadNetwork( final String filename ) throws IOException
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
            Color color = Color.decode( node.getString( "color" ) );
            
            addNode( xPos, yPos, id, name, delay, color );
        }
        
        // Get the list of links.
        JSONArray links = settings.getJSONArray( "links" );
        length = links.length();
        for(int i = 0; i < length; i++) {
            JSONObject link  = links.getJSONObject( i );
            long fromId      = link.getLong( NetworkLink.FROM_ID );
            long destId      = link.getLong( NetworkLink.DEST_ID );
            double bandwidth = link.getDouble( NetworkLink.BANDWITH );
            long delay       = link.getLong( NetworkLink.DELAY );
            String linkType  = (link.has( NetworkLink.LINK_TYPE )) ?
                                 link.getString( NetworkLink.LINK_TYPE ) :
                                 NetworkLink.UNIDIRECTIONAL;
            
            addLink( fromId, destId, bandwidth, delay );
            
            if(linkType.equals( NetworkLink.BIDIRECTIONAL )) {
                addLink( destId, fromId, bandwidth, delay );
            }
        }
        
        br.close();
    }
    
    public void loadSimulation( final String networkFile, final String packetFile ) throws IOException
    {
        loadNetwork( networkFile );
        loadPackets( packetFile );
    }

    @Override
    public void init( final GameContainer gc ) throws SlickException
    {
        ob = new OptionBar( gc );
        am = new AnimationManager( gc, ob.getMaxY(), width, height );
        nd = new NetworkDisplay( width, height*100/142, am.getMaxY(), nodes, packets, timeDuration );
        ta = new TimeAnimation( gc, nd.getMaxY(), width, height, timeDuration );
    }
    
    public void addNode( final int x, final int y, final long nodeID, final String name, final long delay, final Color color ) {
        Node node = new Node( x, y, nodeID, name, delay, color );
        nodes.add( node );
    }
    
    public void addPacket( final long from_ID, final long dest_ID, final Color color, final long startTime, final long endTime, final int type ) {
        Packet packet = new Packet( getNode( from_ID ), getNode( dest_ID ), color, startTime, endTime, width, height, type );
        packet.setSpeed( AnimationManager.frames );
        packets.add( packet );
    }
    
    public void addLink( final long source, final long dest, final double bandwidth, final long delay ) {
        Node node1 = getNode( source ), node2 = getNode( dest );
        node1.addLink( node2, node1.getCenterX(), node1.getCenterY(), node2.getCenterX(), node2.getCenterY(), width, height );
    }
    
    private Node getNode( final long nodeID )
    {
        for (Node node: nodes) {
            if (node.getNodeID() == nodeID) {
                return node;
            }
        }
        
        return null;
    }

    @Override
    public void update( final GameContainer gc, final int delta ) throws SlickException
    {
        leftMouse = gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON );

        nd.update( gc, am );
        ob.update( delta, gc.getInput(), leftMouse, ob, am, ta, nd );
        am.update( delta, gc.getInput(), leftMouse, ob, am, ta, nd );
        ta.update( delta, gc.getInput(), leftMouse, ob, am, ta, nd );
    }

    @Override
    public void render( final GameContainer gc, final Graphics g ) throws SlickException
    {
        ob.render( gc );
        am.render( gc );
        nd.render( gc );
        ta.render( gc );
    }
}