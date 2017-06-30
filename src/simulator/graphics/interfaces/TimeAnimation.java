
package simulator.graphics.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.ArrowButton;

public class TimeAnimation implements AnimationInterface
{
    private Rectangle barTiming, timing, cursor;
    private long timer, timeDuration;
    
    private int tick = 0, moving = 1, index = -1;

    private int mouseX, mouseY;
    
    private float startTimingX;
    
    private float widthCursor;
    
    private final float height;
    private final float offsetH;
    
    private Rectangle mouse, timeMoving;
    
    private ArrowButton timeOn, timeBack;
    private List<ArrowButton> arrows;
    
    private boolean buttonHit = false, timingHit = false;
    
    public TimeAnimation( final float startY, final float width, final float height, final long timeDuration ) throws SlickException
    {
        this.height = height;
        
        arrows = new ArrayList<ArrowButton>();
        
        startTimingX = width/16;
        widthCursor  = width/150;
        
        offsetH = height/120;
        
        barTiming = new Rectangle( 0, startY, width, height*10/75 );
        timing    = new Rectangle( startTimingX, startY, width - 2*startTimingX, height/24 );
        cursor    = new Rectangle( startTimingX - widthCursor/2, timing.getY(), widthCursor, timing.getHeight() );
        
        mouse = new Rectangle( 0, 0, 1, 1 );
        
        float widthTimeMoving = width/40, heightTimeMoving = height/30;
        timeMoving = new Rectangle( timing.getCenterX() - widthTimeMoving/2, timing.getMaxY() + height/150, widthTimeMoving, heightTimeMoving );
        
        float distance = width/40;
        timeOn   = new ArrowButton( "ON", ArrowButton.RIGHT, new float[]{timeMoving.getMaxX() + distance, timeMoving.getY(),
				   													   timeMoving.getMaxX() + distance*3, timeMoving.getCenterY(),
				   													   timeMoving.getMaxX() + distance, timeMoving.getMaxY()},
        							Color.green, 0 );
        timeBack = new ArrowButton( "BACK", ArrowButton.LEFT, new float[]{timeMoving.getX() - distance*3, timeMoving.getCenterY(),
				   													  timeMoving.getX() - distance, timeMoving.getY(),
				   													  timeMoving.getX() - distance, timeMoving.getMaxY()},
        						    Color.green, 1 );
        
        arrows.add( timeOn );
        arrows.add( timeBack );
        
        this.timeDuration = timeDuration;
    }
    
    private long roundValue( final double value ) {
        return (long) (Math.round( value * 100.0 ) / 100.0);
    }
    
    private void setTime( final NetworkDisplay nd ) {
        cursor.setX( Math.max( Math.min( mouseX - widthCursor/2, timing.getMaxX() - widthCursor/2 ), startTimingX - widthCursor/2 ) );
        nd.setTimeSimulation( roundValue( (((double) cursor.getCenterX() - startTimingX) / timing.getWidth() * timeDuration) ) );
    }
    
    public String toString( final float mouseX ) {
        return roundValue( (((double) Math.max( Math.min( mouseX, timing.getMaxX() ), startTimingX ) - startTimingX) / timing.getWidth() * timeDuration) ) + "";
    }
    
    private void setCursor( final int index, final NetworkDisplay nd ) {
    	if (index == 0) {
    		timer = Math.min( timer + moving, timeDuration );
    	} else {
    		timer = Math.max( timer - moving, 0 );
    	}
    	
    	nd.setTimeSimulation( timer );
    }
    
    @Override
    public void update( final int delta, final GameContainer gc, final boolean leftMouse, final NetworkDisplay nd )
    {
        mouseX = gc.getInput().getMouseX();
        mouseY = gc.getInput().getMouseY();
        Graphics g = gc.getGraphics();
        
        mouse.setLocation( mouseX, mouseY );
        
        timer = nd.getTimeSimulation();

        if (index >= 0) {
        	ArrowButton arrow = arrows.get( index );
            if (arrow.contains( mouseX, mouseY ) && ++tick >= 50) {
                setCursor( index, nd );
            }
        }
        
        if (leftMouse) {
        	if (timingHit) {
        		setTime( nd );
        	} else if (timing.intersects( mouse )) {
    			timingHit = true;
        		setTime( nd );
    		} else if (!buttonHit) {
    			for (ArrowButton arrow: arrows) {
					if (arrow.contains( mouseX, mouseY )) {
						arrow.setPressed( true );
						buttonHit = true;
						index = arrow.getIndex();
						setCursor( index, nd );
					}
    			}
			}
        } else if (buttonHit) {
    		arrows.get( index ).setPressed( false );
        	buttonHit = false;
    		index = -1;
    		tick = 0;
    	} else if (timingHit) {
    		timingHit = false;
    		setTime( nd );
    	}
        
        if (timing.intersects( mouse ) || timingHit) {
        	String info = toString( mouseX );
            float fontW = g.getFont().getWidth( info );
            NetworkDisplay.info.setAttributes( g, info, Math.max( Math.min( mouseX, timing.getMaxX() ), timing.getX() ) - fontW/2, timing.getMaxY() + offsetH );
        }
        
        cursor.setX( startTimingX - widthCursor/2 + timing.getWidth() / timeDuration * timer );
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
        
        String info = timer + "/" + timeDuration;
        int fWidth = g.getFont().getWidth( info ), fHeight = g.getFont().getHeight( info );
        g.drawString( info, timing.getCenterX() - fWidth/2, timing.getMaxY() + (height - timing.getMaxY() - fHeight)/2 );
        
        g.fill( timeMoving );
        g.setColor( Color.black );
        fWidth = g.getFont().getWidth( moving + "" ); fHeight = g.getFont().getHeight( moving + "" );
        g.drawString( moving + "", timeMoving.getCenterX() - fWidth/2, timeMoving.getCenterY() - fHeight/2 );
        
        for (ArrowButton arrow: arrows) {
        	arrow.draw( g );
        }
    }
}