
package simulator.graphics;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;

import simulator.graphics.interfaces.AnimationManager;
import simulator.graphics.interfaces.OptionBar;
import simulator.graphics.interfaces.TimeAnimation;

public class AnimationNetwork extends BasicGame
{
    private OptionBar ob;
    private AnimationManager am;
    private TimeAnimation ta;
    
    private boolean leftMouse;
    
    public AnimationNetwork( final String title )
    {
        super( title );
    }

    @Override
    public void init( final GameContainer gc ) throws SlickException
    {
        ob = new OptionBar( gc );
        am = new AnimationManager( gc, ob.getMaxY() );
        ta = new TimeAnimation();
    }

    @Override
    public void update( final GameContainer gc, final int delta ) throws SlickException
    {
    	leftMouse = gc.getInput().isMousePressed( Input.MOUSE_LEFT_BUTTON );
    	
    	ob.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta );
    	am.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta );
    	ta.update( gc, delta, gc.getInput(), leftMouse, ob, am, ta );
    }

    @Override
    public void render( final GameContainer gc, final Graphics g ) throws SlickException
    {
        ob.render( g );
        am.render( g );
        ta.render( g );
    }

}