
package simulator.graphics;

import java.util.ArrayList;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

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
    
    private Node node1, node2, node3, node4;
    private Packet packet;
    
    private float offset;
    
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
        
        // TODO FARE UNA PROVA DI LETTURA DA FILE
        
        //TESTING
        node1 = new Node( 150, 150, 0, 1, Color.black, offset );
        node2 = new Node( 300, 300, 1, 2, Color.green, offset );
        node3 = new Node( 450, 300, 2, 3, Color.red, offset );
        node4 = new Node( 600, 150, 3, 3, Color.yellow, offset );
        
        nodes.add( node1 );
        nodes.add( node2 );
        nodes.add( node3 );
        nodes.add( node4 );
        
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