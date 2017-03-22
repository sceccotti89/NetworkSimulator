
package simulator.graphics.interfaces;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Input;

public interface AnimationInterface
{    
    public void render( final GameContainer gc );

	public void update( final GameContainer gc, final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta, final NetworkDisplay nd );
}
