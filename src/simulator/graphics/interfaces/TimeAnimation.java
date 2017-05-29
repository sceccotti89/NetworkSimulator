
package simulator.graphics.interfaces;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.geom.Rectangle;

public class TimeAnimation implements AnimationInterface
{
    private Rectangle barTiming  = new Rectangle( 0, 520, 800, 80 );
    
    @Override
    public void update( final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta, final NetworkDisplay nd )
    {
        
    }
    
    @Override
    public void render( final GameContainer gc )
    {
    	Graphics g = gc.getGraphics();
    	
    	g.setColor( Color.black );
        g.draw( barTiming );
    }
    
    public float getY(){
    	return barTiming.getY();
    }
}