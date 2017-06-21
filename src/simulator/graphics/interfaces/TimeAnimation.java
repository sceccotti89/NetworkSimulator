
package simulator.graphics.interfaces;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

public class TimeAnimation implements AnimationInterface
{
    private Rectangle barTiming, timing, cursor;
    
    private long timeDuration;
    
    private int mouseX, mouseY;
    
    private boolean mouseDown, cursorHit = false;
    
    public TimeAnimation( final GameContainer gc, final float startY, final float width, final float height, final long timeDuration ) throws SlickException
    {
    	barTiming = new Rectangle( 0, startY, width, height*10/75 );
    	timing    = new Rectangle( 0, startY, width, height*10/225 );
    	cursor    = new Rectangle( 100, timing.getY(), width/130,timing.getHeight()  );
    	
    	this.timeDuration = timeDuration;
    }
    
    @Override
    public void update( final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta, final NetworkDisplay nd )
    {
    	mouseX = input.getMouseX();
		mouseY = input.getMouseY();
		
		if (leftMouse && !mouseDown) {
            mouseDown = true;
            
            if (cursor.contains( mouseX, mouseY )) {
            	cursorHit = true;
            }
		} else if (!leftMouse && mouseDown) {
            mouseDown = false;
            
            if (timing.contains( mouseX, mouseY )) {
            	cursor.setLocation( mouseX - cursor.getWidth()/2, cursor.getY() );
            }
            
            cursorHit = false;
		}
		
		if (cursorHit && mouseDown) {
			cursor.setLocation( mouseX - cursor.getWidth()/2, cursor.getY() );
		}
    }
    
    @Override
    public void render( final GameContainer gc )
    {
    	Graphics g = gc.getGraphics();
    	
    	g.setColor( Color.black );
        g.draw( barTiming );
        
        g.setColor( Color.red );
        g.fill( timing );
        
        g.setColor( Color.white );
        g.fill( cursor );
    }
    
    public float getY(){
    	return barTiming.getY();
    }
}