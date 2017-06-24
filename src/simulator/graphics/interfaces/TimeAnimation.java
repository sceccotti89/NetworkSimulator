
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
    
    private float widthCursor;
    
    public TimeAnimation( final GameContainer gc, final float startY, final float width, final float height, final long timeDuration ) throws SlickException
    {
        startTimingX = width/80;
        widthCursor = width/150;
        
        barTiming = new Rectangle( 0, startY, width, height*10/75 );
        timing    = new Rectangle( startTimingX, startY, width - 2*startTimingX, height*10/225 );
        cursor    = new Rectangle( startTimingX - widthCursor/2, timing.getY(), widthCursor, timing.getHeight() );
        
        this.timeDuration = timeDuration;
    }
    
    private void setTime( NetworkDisplay nd ) {
        cursor.setX( Math.max( Math.min( mouseX - widthCursor/2, timing.getMaxX() - widthCursor/2 ), startTimingX - widthCursor/2 ) );
        nd.setTimeSimulation( (long) (((double) cursor.getCenterX() - startTimingX) / timing.getWidth() * timeDuration) );
        nd.checkActivityPackets();
    }
    
    @Override
    public void update( final int delta, final Input input, final boolean leftMouse, OptionBar ob, AnimationManager am, TimeAnimation ta, NetworkDisplay nd )
    {
        mouseX = input.getMouseX();
        mouseY = input.getMouseY();
        
        timer = nd.getTimeSimulation();
        
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
                        setTime( nd );
                    }
                }
                
                cursorHit = false;
            } else if (cursorHit && mouseDown) {
            	setTime( nd );
            }
        } else {
        	cursor.setX( startTimingX - widthCursor/2 + timing.getWidth() / timeDuration * timer );
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
        
        g.setColor( Color.white );
        String info = timer + "/" + timeDuration;
        int fWidth = g.getFont().getWidth( info ), fHeight = g.getFont().getHeight( info );
        g.drawString( info, timing.getCenterX() - fWidth/2, barTiming.getMaxY() - (barTiming.getMaxY() - timing.getMaxY())/2 - fHeight/2 );
    }
}