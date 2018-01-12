
package simulator.graphics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

import simulator.graphics.elements.Event;
import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;
import simulator.graphics.interfaces.AnimationInterface;
import simulator.graphics.interfaces.AnimationManager;
import simulator.graphics.interfaces.NetworkDisplay;
import simulator.graphics.interfaces.OptionBar;
import simulator.graphics.interfaces.TimeAnimation;
import simulator.topology.NetworkLink;
import simulator.topology.NetworkNode;

public class AnimationNetwork extends AppGameContainer
{
    private List<Node> nodes;
    private List<Packet> packets;
    
    public static long timeSimulation = 0;
	private final int limit = 1000000;
	
	private static Animator anim;
	
	public static int tick = 0;
    
    public AnimationNetwork( int width, int height, String title ) throws SlickException
    {
        super( anim = new Animator( width, height, title ), width, height, false );
        
        setTargetFrameRate( 30 );
        
        nodes   = new ArrayList<Node>();
        packets = new ArrayList<Packet>();
        
        anim.setAnimationElements( nodes, packets );
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
            Node from = getNode( from_ID );
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
        if (timeSimulation >= limit ) {
        	measure = "TIME";
        }
        for (Packet packet: packets) {
        	packet.setMeasure( measure );
        }
        
        System.out.println( "Loading completed." );
    }
    
    public void loadNetwork( String filename ) throws IOException, SlickException
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
        int index = 0;
        for(int i = 0; i < length; i++) {
            JSONObject node = nodes.getJSONObject( i );
            long id     = node.getLong( NetworkNode.ID );
            String name = node.getString( NetworkNode.NAME );
            long delay  = node.getLong( NetworkNode.DELAY );
            int xPos    = (node.has( NetworkNode.X_POS )) ? node.getInt( NetworkNode.X_POS ) : 0;
            int yPos    = (node.has( NetworkNode.Y_POS )) ? node.getInt( NetworkNode.Y_POS ) : 0;
            Color color = (node.has( "color" )) ? Color.decode( node.getString( "color" ) ) : Color.black;
            
            addNode( xPos, yPos, id, name, delay, color, index++ );
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
            
            if(linkType.equals( NetworkLink.BIDIRECTIONAL )) {
                addLink( destId, fromId, bandwidth, delay, linkType );
            }
        }
        
        br.close();
    }
    
    public void addNode( int x, int y, long nodeID, String name, long delay, Color color, int index ) throws SlickException {
        Node node = new Node( x, y, nodeID, name, delay, color, index );
        nodes.add( node );
    }
    
    public void addPacket( long from_ID, long dest_ID, Color color, long startTime, long endTime, int type ) {
        Packet packet = new Packet( getNode( from_ID ), getNode( dest_ID ), color, startTime, endTime, width, height, type );
        packets.add( packet );
    }
    
    public void addLink( long source, long dest, double bandwidth, long delay, String type ) {
        Node node1 = getNode( source );
        node1.addLink( getNode( dest ), bandwidth, delay, width, height, type );
    }
    
    private Node getNode( long nodeID )
    {
        for (Node node: nodes) {
            if (node.getNodeID() == nodeID) {
                return node;
            }
        }
        
        return null;
    }
    
    public void loadSimulation( String networkFile, String packetFile ) throws IOException, SlickException
    {
        loadNetwork( networkFile );
        loadPackets( packetFile );
    }
    
    private static class Animator extends BasicGame
    {
        protected OptionBar ob;
        protected AnimationManager am;
        protected TimeAnimation ta;
        protected NetworkDisplay nd; 
        
        protected boolean mouseEvent = false;
    	
    	private Event event;
        
        private List<Node> nodes;
        private List<Packet> packets;
        
        private int width;
        private int height;
        
        private final List<AnimationInterface> interfaces;
    	
    	private Input lastInput = null;
        
        public Animator( int width, int height, String title )
        {
            super( title );
            
            this.width = width;
            this.height = height;
            
            event = new Event();
            
            interfaces = new ArrayList<AnimationInterface>();
        }
        
        private void setAnimationElements( List<Node> nodes, List<Packet> packets ) {
            this.nodes = nodes;
            this.packets = packets;
        }
        
        @Override
        public void init( GameContainer gc ) throws SlickException
        {
        	ob = new OptionBar( gc, width, height );
            am = new AnimationManager( gc, ob.getMaxY(), width, height );
            nd = new NetworkDisplay( width, height, am.getMaxY(), nodes, packets );
            ta = new TimeAnimation( nd.getMaxY(), width, height );
            
            nd.nodeInit();
            event.setInput( gc.getInput() );
            
            interfaces.add( ob );
            interfaces.add( am );
            interfaces.add( nd );
            interfaces.add( ta );
        }
        
        private boolean evaluateEventMouse( Input input ) {
        	return (input.isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )
        		 || input.isMouseButtonDown( Input.MOUSE_RIGHT_BUTTON ));
        }
        
        @Override
        public void update( GameContainer gc, int delta ) throws SlickException
        {
        	if (mouseEvent) {
        		event.setInput( gc.getInput() );
        	}
        	
        	if (!mouseEvent && !evaluateEventMouse( gc.getInput() )) {
        		lastInput = null;
        	}
        	
        	// TODO IMPOSTARE IL CONTROLLO DEL CLICK SUL RILASCIO DI ESSO
        	if (!mouseEvent && evaluateEventMouse( gc.getInput() ) && gc.getInput() != lastInput) {
        		mouseEvent = true;
        		event.setInput( gc.getInput() );
        		event.setConsumed( false );
        		lastInput = gc.getInput();
        	} else if (mouseEvent) {
        		if (!evaluateEventMouse( gc.getInput() )) {
        			lastInput = null;
        			mouseEvent = false;
        		}
        	}
        	
        	// checkClick
        	if (mouseEvent) {
	        	for (int i = 0; i < interfaces.size(); i++) {
	        		AnimationInterface obj = interfaces.get( i );
	        		if (obj.checkClick( event, nd )) {
	        			if (i > 0) {
	        				interfaces.remove( obj );
	        				interfaces.add( 0, obj );
	        			}
	        			
	        			mouseEvent = false;
	        			tick = 0;
	        			
	        			break;
	        		}
	        	}
        	}
        	
        	// update delle interfacce
        	for (AnimationInterface obj: interfaces) {
        		obj.update( delta, gc, am, event, nd, mouseEvent );
        	}
        }
        
        @Override
        public void render( GameContainer gc, Graphics g ) throws SlickException
        {
            am.render( gc );
            ta.render( gc );
            nd.render( gc );
            ob.render( gc );
        }
    }
}
