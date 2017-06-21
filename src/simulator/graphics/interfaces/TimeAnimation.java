
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
    
    private boolean mouseDown, cursorHit = false, timingHit = false;
    
    private float startTimingX;
    
    private long timer;
    
    public TimeAnimation( final GameContainer gc, final float startY, final float width, final float height, final long timeDuration ) throws SlickException
    {
    	startTimingX = width/80;
    	
    	barTiming = new Rectangle( 0, startY, width, height*10/75 );
    	timing    = new Rectangle( startTimingX, startY, width - 2*startTimingX, height*10/225 );
    	cursor    = new Rectangle( timing.getX(), timing.getY(), width/150, timing.getHeight() );
    	
    	this.timeDuration = timeDuration;
    }
    
    @Override
    public void update( final int delta, final Input input, boolean leftMouse, OptionBar ob, AnimationManager am, TimeAnimation ta, NetworkDisplay nd )
    {
    	mouseX = input.getMouseX();
		mouseY = input.getMouseY();
		
		if (leftMouse && !mouseDown) {
            mouseDown = true;
            
            if (cursor.contains( mouseX, mouseY )) {
            	cursorHit = true;
            } else if (timing.contains( mouseX, mouseY )) {
            	timingHit = true;
            } else {
            	mouseDown = false;
            }
		} else if (mouseDown) {
			if (!leftMouse) {
	            mouseDown = false;
	            
	            if (timingHit || cursorHit) {
		            if (timing.contains( mouseX, mouseY )) {
		            	cursor.setX( Math.max( Math.min( mouseX - cursor.getWidth()/2, timing.getMaxX() ), timing.getX() ) );
		            	nd.setTiminigSimulator( (long) ((mouseX - startTimingX)/(timing.getWidth()/timeDuration)) );
		            }
	            }
	            
	            cursorHit = false;
			} else if (cursorHit && mouseDown) {
				cursor.setX( Math.max( Math.min( mouseX - cursor.getWidth()/2, timing.getMaxX() ), timing.getX() ) );
            	nd.setTiminigSimulator( (long) ((mouseX - startTimingX)/(timing.getWidth()/timeDuration)) );
			}
		}
		
		timer = nd.getTimingSimulation();
		
		cursor.setX( timing.getX() + timing.getWidth()/timeDuration*timer );
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
        
        g.setColor( Color.white );
        String info = timer + "/" + timeDuration;
        int fWidth = g.getFont().getWidth( info ), fHeight = g.getFont().getHeight( info );
        g.drawString( info, timing.getX()/2 + timing.getWidth()/2 - fWidth/2, barTiming.getMaxY() - (barTiming.getMaxY() - timing.getMaxY())/2 - fHeight/2 );
    }
    
    public float getY(){
    	return barTiming.getY();
    }
}