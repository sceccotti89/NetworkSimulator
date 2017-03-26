
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
    private Packet packet;
    
    private Node node1, node2, node3;
    
    public AnimationNetwork( final String title )
    {
        super( title );
    }

    @Override
    public void init( final GameContainer gc ) throws SlickException
    {        
        nodes = new ArrayList<Node>();
        
        //TESTING OF THE DRAW
        node1 = new Node( 150, 150, 0, 1, Color.black );
        node2 = new Node( 300, 150, 1, 2, Color.green );
        node3 = new Node( 450, 150, 2, 2, Color.red );
        
        nodes.add( node1 );
        nodes.add( node2 );
        nodes.add( node3 );
        
        packet = new Packet( gc, nodes.get( 0 ).getCenterX(), nodes.get( 0 ).getCenterY(), 0, 2, nodes.get( 0 ).getColor() );
        
        ob = new OptionBar( gc );
        am = new AnimationManager( gc, ob.getMaxY() );
        ta = new TimeAnimation();
        nd = new NetworkDisplay( gc, am.getMaxY(), ta.getY() - am.getMaxY(), nodes, packet );
    }

    @Override
    public void update( final GameContainer gc, final int delta ) throws SlickException
    {
    	leftMouse = gc.getInput().isMousePressed( Input.MOUSE_LEFT_BUTTON );
    	
    	ob.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta, nd );
    	am.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta, nd );
    	ta.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta, nd );
		
		if(nd.getAnimate())
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