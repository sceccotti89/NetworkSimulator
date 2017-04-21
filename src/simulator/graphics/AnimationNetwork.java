
package simulator.graphics;

import java.io.File;
import java.util.ArrayList;

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

import simulator.elements.Node;
import simulator.elements.Packet;
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
    
    private ArrayList<Node> nodes;
    private ArrayList<Packet> packets;
    
    private Node node, node1, node2, node3, node4;
    private Packet packet;
    
    private float offset;
    
    private DocumentBuilderFactory documentFactory;
	private DocumentBuilder builder;
	private Document document;
    
    public AnimationNetwork( final String title )
    {
        super( title );
    }
    
    public void sortPackets() {
    	Packet tmpi, tmpj;
    	
    	for (int i = 0; i < packets.size() - 1; i++) {
    		for (int j = i + 1; j < packets.size(); j++) {
    			if (packets.get( j ).getTime() < packets.get( i ).getTime()) {
    				tmpi = packets.get( i );
    				tmpj = packets.get( j );
    				
    				packets.remove( i );
    				packets.remove( j );
    				
    				packets.add( i, tmpj );
    				packets.add( j, tmpi );
    			}
    		}
    	}
    }

    @Override
    public void init( final GameContainer gc ) throws SlickException
    {        
        nodes   = new ArrayList<Node>();
        packets = new ArrayList<Packet>();
        
        offset = gc.getWidth()/100;

        try {
			documentFactory = DocumentBuilderFactory.newInstance();
 
			builder = documentFactory.newDocumentBuilder();
			
			/* LETTURA FILE CONFIGURAZIONE TASTI */
			File levels = new File( "data/File/" );
			String[] files = levels.list();
				
			document = builder.parse( new File( "data/File/" + files[0] ) );

			NodeList config = document.getElementsByTagName( "node" );
			
			for (int i = 0; i < config.getLength(); i++) {
				org.w3c.dom.Node nodo = config.item( i );
				Element obj = (Element) nodo;
				
				node = new Node( Integer.parseInt( obj.getAttribute( "x" ).substring( 0, obj.getAttribute( "x" ).length() - 2 ) ),
						Integer.parseInt( obj.getAttribute( "y" ).substring( 0, obj.getAttribute( "x" ).length() - 2 ) ),
						Integer.parseInt( obj.getAttribute( "from" ) ),
						Integer.parseInt( obj.getAttribute( "to" ) ),
						Color.black,
						offset );
				
				nodes.add( node );
			}
			
			System.out.println( "NODI = " + nodes.size() );
			
			System.out.println( "nodi " + files[0] + " caricati" );
		}
        catch(Exception e) {
        	e.printStackTrace();
        }
        
        for (int i = 0; i < nodes.size() - 1; i++) {
            Node node1 = nodes.get( i ), node2 = nodes.get( i + 1 );
            node1.createLink( node1.getArea().getCenterX(), node1.getArea().getCenterY(), node2.getArea().getCenterX(), node2.getArea().getCenterY(), node2.getColor() );
        }
        
        packet = new Packet( gc, nodes.get( 0 ).getCenterX(), nodes.get( 0 ).getCenterY() + gc.getWidth()/50, 0, 1, nodes.get( 0 ).getColor(), 5 );
        
        packets.add( packet );
        
        sortPackets();
        
        for (Packet packet: packets) {
        	packet.setSpeed( nodes.get( packet.getIndexRotation() ).getAngle() );
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