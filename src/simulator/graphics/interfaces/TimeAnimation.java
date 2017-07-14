
package simulator.graphics.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.AnimationNetwork;
import simulator.graphics.dataButton.ArrowButton;
import simulator.graphics.elements.Time;

public class TimeAnimation implements AnimationInterface
{
    private final float OFFSET;
	private final Rectangle barTiming, timing, cursor;
    private long timer;
    
    private int tick = 0, moving = 1, index = -1;

    private int mouseX, mouseY;
    
    private final int limit = 1000000;
    
    private final float startTimingX;
    
    private final float widthCursor;
    
    private final float width, height;
    private final float offsetH;
    
    private final Rectangle mouse;
    
    private final ArrowButton timeOn, timeBack;
    private final List<ArrowButton> arrows;
    
    private boolean buttonHit = false, timingHit = false;
    
    private final Time timeUs, timeS;
	private boolean mouseDown;
    
    public TimeAnimation( float startY, final float width, final float height ) throws SlickException
    {
    	this.width = width;
        this.height = height;
        
        OFFSET = width/40;
        
        arrows = new ArrayList<ArrowButton>();
        
        startTimingX = width/16;
        widthCursor  = width/150;
        
        offsetH = height/120;
        
        barTiming = new Rectangle( 0, startY, width, height*10/75 );
        timing    = new Rectangle( startTimingX, startY, width - 2*startTimingX, height/24 );
        cursor    = new Rectangle( startTimingX - widthCursor/2, timing.getY(), widthCursor, timing.getHeight() );
        
        mouse = new Rectangle( 0, 0, 1, 1 );
        
        float distX = width*10/37, distY = height/70, widthA = width/10*(15/10);
        float centerX = timing.getCenterX(), centerY = timing.getMaxY() + (height - timing.getMaxY())/2;
        timeOn   = new ArrowButton( "ON", ArrowButton.RIGHT, new float[]{centerX + distX, centerY - distY,
        																 centerX + distX + widthA, centerY,
        																 centerX + distX, centerY + distY},
        							Color.green, 0 );
        timeBack = new ArrowButton( "BACK", ArrowButton.LEFT, new float[]{centerX - distX - widthA, centerY,
        																  centerX - distX, centerY - distY,
        																  centerX - distX, centerY + distY},
        						    Color.green, 1 );
        
        arrows.add( timeOn );
        arrows.add( timeBack );
        
        final float widthT = width/53, heightT = height/40;
        startY = timing.getMaxY() + (height - timing.getMaxY())*7/8 - heightT/2;
        timeUs = new Time( width/2 - widthT/2 - width/16, startY - heightT/2, widthT, heightT, "탎" );
        timeS  = new Time( width/2 - widthT/2 + width/16, startY - heightT/2 , widthT, heightT, "h:m:s" );
    }
    
    private long roundValue( final double value ) {
        return (long) (Math.round( value * 100.0 ) / 100.0);
    }
    
    private void setTime( final NetworkDisplay nd ) {
        cursor.setX( Math.max( Math.min( mouseX - widthCursor/2, timing.getMaxX() - widthCursor/2 ), startTimingX - widthCursor/2 ) );
        nd.setTimeSimulation( roundValue( (((double) cursor.getCenterX() - startTimingX) / timing.getWidth() * AnimationNetwork.timeSimulation) ) );
    }
    
    public long getTime( final float mouseX ) {
        return roundValue( (((double) Math.max( Math.min( mouseX, timing.getMaxX() ), startTimingX ) - startTimingX) / timing.getWidth() * AnimationNetwork.timeSimulation) );
    }
    
    private void setCursor( final int index, final NetworkDisplay nd ) {
    	if (index == 0) {
    		timer = Math.min( timer + moving, AnimationNetwork.timeSimulation );
    	} else {
    		timer = Math.max( timer - moving, 0 );
    	}
    	
    	nd.setTimeSimulation( timer );
    }
    
    private void setPositionArrows( final float widthS ) {
    	for (ArrowButton arrow: arrows) {
    		arrow.setX( width/2, widthS );
    	}
    }
    
    private String setTime( long time, final GameContainer gc ) {
    	long decimal = time;
    	time = time / limit;
    	long h = time/3600, m = (time - h*3600)/60, s = time - h*3600 - m*60,
       		 ms = (decimal - (h*3600 + m*60 + s) * limit) / 1000, ns = (decimal - (h*3600 + m*60 + s) * limit) - ms * 1000;
    	
       	String info = "";
       	
       	if (timeS.isSelected()) {
       		info = h + "h:";
	       	if (m < 10) {
	       		info = info + "0" + m + "m:";
	       	} else {
	       		info = info + m + "m:";
	       	}
	       	
	       	if (s < 10) {
	       		info = info + "0" + s + "s";
	       	} else {
	       		info = info + s + "s";
	       	}
	       	if (timeUs.isSelected()) {
	       		info = info + ":";
	       	}
       	} else if (timeUs.isSelected()) {
       		ms = decimal/1000;
       		if (ms < 10) {
	   			info = info + "00" + ms + "ms:";
	       	} else if (ms < 100) {
	       		info = info + "0" + ms + "ms:";
	       	} else {
	       		info = info + ms + "ms:";
	       	}
       		
       		ns = decimal - ms*1000;
       		if (ns < 10) {
	   			info = info + "00" + ns + "탎";
	       	} else if (ns < 100) {
	       		info = info + "0" + ns + "탎";
	       	} else {
	       		info = info + ns + "탎";
	       	}
           	
       		return info;
       	}
       	
       	if (timeUs.isSelected()) {
	       	if (ms < 10) {
	   			info = info + "00" + ms + "ms:";
	       	} else if (ms < 100) {
	       		info = info + "0" + ms + "ms:";
	       	} else {
	       		info = info + ms + "ms:";
	       	}
	       	
	       	if (ns < 10) {
	   			info = info + "00" + ns + "탎";
	       	} else if (ns < 100) {
	       		info = info + "0" + ns + "탎";
	       	} else {
	       		info = info + ns + "탎";
	       	}
       	}
       	
       	/*if (gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
       	    System.out.println( "NS = " + ns );
       	}*/
       	
    	return info;
    }
    
    @Override
    public void update( final int delta, final GameContainer gc, final boolean leftMouse, final NetworkDisplay nd )
    {
        mouseX = gc.getInput().getMouseX();
        mouseY = gc.getInput().getMouseY();
        Graphics g = gc.getGraphics();
        
        mouse.setLocation( mouseX, mouseY );
        
        timer = nd.getTimeSimulation();
        
        if (buttonHit) {
        	ArrowButton arrow = arrows.get( index );
            if (arrow.contains( mouseX, mouseY ) && ++tick >= 50) {
                setCursor( index, nd );
            }
        }
        
        if (leftMouse && !mouseDown) {
            mouseDown = true;
        } else if (!leftMouse && mouseDown) {
            mouseDown = false;
           	
            if (timeUs.checkClick( mouseX, mouseY )) {
            	if (timeS.isSelected()) {
                	timeUs.setSelected();
            	}
            	
            	if (timeUs.isSelected()) {
            		moving = 1;
            	} else {
            		moving = 1 * limit;
            	}
            } else if (timeS.checkClick( mouseX, mouseY )) {
            	if (timeUs.isSelected()) {
            		timeS.setSelected();
            	}

            	if (timeUs.isSelected()) {
            		moving = 1;
            	} else {
            		moving = 1 * limit;
            	}
            }
            
            if (buttonHit && index != -1) {
        		buttonHit = false;
        		arrows.get( index ).setPressed( false );
        		index = -1;
        		tick = 0;
        	} else if (timingHit) {
        		timingHit = false;
        		setTime( nd );
        	}
        } else if (leftMouse) {
        	if (timingHit) {
        		setTime( nd );
        	} 
        	
        	if (!buttonHit && !timingHit) {
        		if (timing.intersects( mouse )) {
	    			timingHit = true;
	        		setTime( nd );
	    		} else if (nd.isInPause()) {
	    			for (ArrowButton arrow: arrows) {
						if (arrow.contains( mouseX, mouseY )) {
							arrow.setPressed( true );
							buttonHit = true;
							index = arrow.getIndex();
							setCursor( index, nd );
						}
	    			}
				}
        	}
        }
        
        if (timing.intersects( mouse ) || timingHit) {
        	String info = setTime( getTime( mouseX ), gc );
        	
            float fontW = g.getFont().getWidth( info );
            float x = Math.max( Math.min( mouseX, timing.getMaxX() ), startTimingX ) - fontW/2;
            x = Math.max( x, 0 ); x = Math.min( x, width - fontW );
            NetworkDisplay.info.setAttributes( g, info, x, timing.getMaxY() + offsetH );
        }
        
        cursor.setX( startTimingX - widthCursor/2 + timing.getWidth() / AnimationNetwork.timeSimulation * timer );
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
        
        String info = setTime( timer, gc ) + "/" + setTime( AnimationNetwork.timeSimulation, gc );
        
        int fWidth = g.getFont().getWidth( info ), fHeight = g.getFont().getHeight( info );
       	setPositionArrows( fWidth + OFFSET );
        g.drawString( info, timing.getCenterX() - fWidth/2, timing.getMaxY() + (height - timing.getMaxY() - fHeight)/2 );
        
        for (ArrowButton arrow: arrows) {
        	arrow.draw( g );
        }

        timeUs.render( g );
        timeS.render( g );
    }
}