
package simulator.graphics.interfaces;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.SlickException;

import simulator.graphics.elements.Event;

public interface AnimationInterface
{    
    public void render( GameContainer gc );

	public void resetIndex();
    
    public boolean checkClick( Event event, final NetworkDisplay nd )  throws SlickException;

    public void update( int delta, GameContainer gc, AnimationManager am, Event event, NetworkDisplay nd, boolean mouseEvent ) throws SlickException;
}
