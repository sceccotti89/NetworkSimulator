
package simulator.graphics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;
import simulator.graphics.interfaces.AnimationManager;
import simulator.graphics.interfaces.NetworkDisplay;
import simulator.graphics.interfaces.OptionBar;
import simulator.graphics.interfaces.TimeAnimation;

public class AnimationNetwork extends BasicGame
{
    private OptionBar ob;
    private AnimationManager am;
    private TimeAnimation ta;
    private NetworkDisplay nd; 
    
    private boolean leftMouse;
    
    private List<Node> nodes;
    private List<Packet> packets;
    
    private DocumentBuilderFactory documentFactory;
	private DocumentBuilder builder;
	private Document document;
	
	private int width, height;
	
	private Element obj;
	private NodeList config;
    
    public AnimationNetwork( final int width, final int height, final String title )
    {
        super( title );
        
        nodes   = new ArrayList<Node>();
        packets = new ArrayList<Packet>();
        
        this.width = width;
        this.height = height;
    }
    
    private void loadPackets( String file ) {
    	System.out.println( "Loading from " + file + "..." );
    	try {
			documentFactory = DocumentBuilderFactory.newInstance();
 
			builder = documentFactory.newDocumentBuilder();
			
			document = builder.parse( new File( file ) );
			
			/* PACKETS CONFIGURATION */
			config = document.getElementsByTagName( "packet" );
			
			for (int i = 0; i < config.getLength(); i++) {
				org.w3c.dom.Node pack = config.item( i );
				obj = (Element) pack;
				
				Node from = nodes.get( Integer.parseInt( obj.getAttribute( "from" ) ) );

				final int x = from.getCenterX();
				final int y = from.getCenterY() + height/30;
				final long from_ID = Long.parseLong( obj.getAttribute( "from" ) );
				final long dest_ID = Long.parseLong( obj.getAttribute( "to" ) );
				final Color color = from.getColor();
				final int startTime = Integer.parseInt( obj.getAttribute( "startTime" ) );
				final int endTime = Integer.parseInt( obj.getAttribute( "endTime" ) );

		        addPacket( x, y, from_ID, dest_ID, color, startTime, endTime );
			}
			
			System.out.println( "Loading completed." );
    	}
        catch(Exception e) {
        	e.printStackTrace();
        }
    }
    
    private void loadElements( String file ) {
    	System.out.println( "Loading from " + file + "..." );
    	try {
			documentFactory = DocumentBuilderFactory.newInstance();
 
			builder = documentFactory.newDocumentBuilder();
			
			/* NODES CONFIGURATION */
			document = builder.parse( new File( file ) );

			config = document.getElementsByTagName( "node" );
			
			for (int i = 0; i < config.getLength(); i++) {
				org.w3c.dom.Node nodo = config.item( i );
				obj = (Element) nodo;
				
				final int x = Integer.parseInt( obj.getAttribute( "x" ) );
				final int y = Integer.parseInt( obj.getAttribute( "y" ) );
				final long from_ID = Long.parseLong( obj.getAttribute( "ID" ) );
				final Color color = Color.decode( obj.getAttribute( "color" ) );

				addNode( x, y, from_ID, color );
			}

	    	/* LINKS CONFIGURATION */
			config = document.getElementsByTagName( "link" );
			
			for (int i = 0; i < config.getLength(); i++) {
				org.w3c.dom.Node link = config.item( i );
				obj = (Element) link;
				
				final long source = Long.parseLong( obj.getAttribute( "source" ) );
				final long dest = Long.parseLong( obj.getAttribute( "dest" ) );
				final double bandwidth = Double.parseDouble( obj.getAttribute( "bandwidth" ) );
				final long delay = Long.parseLong( obj.getAttribute( "delay" ) );
				final String type = obj.hasAttribute( "type" ) ? obj.getAttribute( "type" ) : "simplex";
				
				addLink( source, dest, bandwidth, delay );
				if (type.equals( "duplex" )) addLink( dest, source, bandwidth, delay );
			}
			
			System.out.println( "Loading completed." );
		}
        catch(Exception e) {
        	e.printStackTrace();
        }
    }
    
    public void loadSimulation( String file1, String file2 ) {
    	loadElements( file1 );
    	loadPackets( file2 );
        
        Collections.sort( packets );
        
        for (Packet packet: packets) {
        	Node start = getNode( packet.getSourceID() );
        	packet.setLinkLenght( start.getLinkLenght( packet.getDestID() ) - 2*start.getRay() );
        	packet.setAngle( start.getAngle( packet.getDestID() ) );
        	//packet.setSpeed( start.getLinkLenght( packet.getDestID() ) - 2*start.getRay(), start.getAngle( packet.getDestID() ) );
        }
    }

    @Override
    public void init( final GameContainer gc ) throws SlickException
    {
        ob = new OptionBar( gc );
        am = new AnimationManager( gc, ob.getMaxY() );
        ta = new TimeAnimation();
        nd = new NetworkDisplay( gc.getWidth(), ta.getY() - am.getMaxY(), am.getMaxY(), nodes, packets );
    }
    
    public void addNode( int x, int y, long nodeID, Color color ) {
    	Node node = new Node( x, y, nodeID, color );
    	nodes.add( node );
    }
    
    public void addPacket( int x, int y, long from_ID, long dest_ID, Color color, int startTime, int endTime ) {
    	Packet packet = new Packet( x, y, from_ID, dest_ID, color, startTime, endTime, width, height );
        packet.initializingSpeed( getNode( from_ID ), getNode( dest_ID ) );
    	packets.add( packet );
    }
    
    public void addLink( final long source, final long dest, final double bandwidth, final long delay ) {
    	Node node1 = getNode( source ), node2 = getNode( dest );
    	node1.addLink( dest, node1.getCenterX(), node1.getCenterY(), node2.getCenterX(), node2.getCenterY(), width, height );
    }
    
    private Node getNode( final long nodeID ) {
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
    	
    	ob.update( delta, gc.getInput(), leftMouse, ob, am, ta, nd );
    	am.update( delta, gc.getInput(), leftMouse, ob, am, ta, nd );
    	ta.update( delta, gc.getInput(), leftMouse, ob, am, ta, nd );
		
		nd.update( gc, am );
    }

    @Override
    public void render( final GameContainer gc, final Graphics g ) throws SlickException
    {
        ob.render( gc );
        am.render( gc );
        ta.render( gc );
        nd.render( gc );
    }
}