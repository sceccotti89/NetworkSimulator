
package simulator.graphics.interfaces;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

public class TimeAnimation implements AnimationInterface
{
    private Rectangle barTiming, timing;
    
    private long timeDuration;
    
    public TimeAnimation( final GameContainer gc, final float startY, final float width, final float height, final long timeDuration ) throws SlickException
    {
    	barTiming = new Rectangle( 0, startY, width, height*10/75 );
    	timing    = new Rectangle( 0, startY, width, height*10/75 );
    	
    	this.timeDuration = timeDuration;
    }
    
    @Override
    public void update( final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta, final NetworkDisplay nd )
    {
        System.out.println( "TIME = " + timeDuration );
    }
    
    @Override
    public void render( final GameContainer gc )
    {
    	Graphics g = gc.getGraphics();
    	
    	/*g.setColor( Color.black );
        g.draw( barTiming );*/
        
        g.setColor( Color.red );
        g.fill( timing );
    }
    
    public float getY(){
    	return barTiming.getY();
    }
}