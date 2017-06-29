
package simulator.graphics.interfaces;

import org.newdawn.slick.GameContainer;

public interface AnimationInterface
{    
    public void render( final GameContainer gc );

    public void update( final int delta, final GameContainer gc, final boolean leftMouse, final NetworkDisplay nd );
}
