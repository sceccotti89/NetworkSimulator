
package simulator.graphics.interfaces;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.SlickException;

import simulator.graphics.elements.Event;

public interface AnimationInterface
{    
    public void render( final GameContainer gc );

	public void resetIndex();
    
    public boolean checkClick( Event event, final NetworkDisplay nd )  throws SlickException;

    public void update( final int delta, final GameContainer gc, final AnimationManager am, final Event event, final NetworkDisplay nd, final boolean mouseEvent ) throws SlickException;
}
