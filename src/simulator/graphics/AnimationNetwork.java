
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
import org.newdawn.slick.geom.Circle;
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
    
    private Node node;
    private Packet packet;
    
    private DocumentBuilderFactory documentFactory;
	private DocumentBuilder builder;
	private Document document;
    
    public AnimationNetwork( final String title )
    {
        super( title );
    }

    @Override
    public void init( final GameContainer gc ) throws SlickException
    {        
        nodes   = new ArrayList<Node>();
        packets = new ArrayList<Packet>();

        try {
			documentFactory = DocumentBuilderFactory.newInstance();
 
			builder = documentFactory.newDocumentBuilder();
			
			/* NODES CONFIGURATION */
			File levels = new File( "data/File/" );
			String[] files = levels.list();
				
			document = builder.parse( new File( "data/File/" + files[0] ) );

			NodeList config = document.getElementsByTagName( "node" );
			
			for (int i = 0; i < config.getLength(); i++) {
				org.w3c.dom.Node nodo = config.item( i );
				Element obj = (Element) nodo;
								
				node = new Node( 
					Integer.parseInt( obj.getAttribute( "x" ).substring( 0, obj.getAttribute( "x" ).length() - 2 ) ),
					Integer.parseInt( obj.getAttribute( "y" ).substring( 0, obj.getAttribute( "x" ).length() - 2 ) ),
					Integer.parseInt( obj.getAttribute( "from" ) ),
					Integer.parseInt( obj.getAttribute( "to" ) ),
					Color.decode( obj.getAttribute( "color" ) ) );
				
				nodes.add( node );
			}
			
			/* PACKETS CONFIGURATION */
			config = document.getElementsByTagName( "packet" );
			
			for (int i = 0; i < config.getLength(); i++) {
				org.w3c.dom.Node pack = config.item( i );
				Element obj = (Element) pack;
				
				Node from = nodes.get( Integer.parseInt( obj.getAttribute( "from" ) ) );
				
				packet = new Packet(
					gc,
					from.getCenterX(),
					from.getCenterY() + gc.getWidth()/50,
					Integer.parseInt( obj.getAttribute( "from" ) ),
					Integer.parseInt( obj.getAttribute( "to" ) ),
					from.getColor(),
					Integer.parseInt( obj.getAttribute( "startTime" ) ),
					Integer.parseInt( obj.getAttribute( "endTime" ) ) );

		        packets.add( packet );
			}
			
			System.out.println( "nodi e pacchetti " + files[0] + " caricati" );
		}
        catch(Exception e) {
        	e.printStackTrace();
        }
        
        for (int i = 0; i < nodes.size() - 1; i++) {
        	Circle areaNode1 = nodes.get( i ).getArea(), areaNode2 = nodes.get( i + 1 ).getArea();
            Node node1 = nodes.get( i ), node2 = nodes.get( i + 1 );
            node1.createLink( gc, areaNode1.getCenterX(), areaNode1.getCenterY(), areaNode2.getCenterX(), areaNode2.getCenterY(), node2.getColor() );
        }
        
        Collections.sort( packets );
        
        for (Packet packet: packets) {
        	Node start = nodes.get( packet.getIndexRotation() );
        	packet.setSpeed( start.getLinkLenght() - 2*start.getRay(), start.getAngle() );
        }
        
        ob = new OptionBar( gc );
        am = new AnimationManager( gc, ob.getMaxY() );
        ta = new TimeAnimation();
        nd = new NetworkDisplay( gc, am.getMaxY(), ta.getY() - am.getMaxY(), nodes, packets );
    }

    @Override
    public void update( final GameContainer gc, final int delta ) throws SlickException
    {
    	leftMouse = gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON );
    	
    	ob.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta, nd );
    	am.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta, nd );
    	ta.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta, nd );
		
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