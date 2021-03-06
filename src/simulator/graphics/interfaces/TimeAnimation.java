
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
import simulator.graphics.elements.CheckBox;
import simulator.graphics.elements.Event;

public class TimeAnimation implements AnimationInterface
{
    private final float OFFSET;
	private final Rectangle barTiming, timing, cursor;
    private long timer;
    
    private int moving = 1, index = -1;

    private int mouseX, mouseY;
    
    private final int limit = 1000000;
    
    private final float startTimingX;
    
    private final float widthCursor;
    
    private final float width, height;
    private final float offsetH;
    
    private final Rectangle mouse;
    
    private final ArrowButton timeOn, timeBack;
    private final List<ArrowButton> arrows;
    
    private boolean arrowHit = false, timingHit = false;
    
    private final CheckBox timeUs, timeS;
    private List<CheckBox> checkBoxes; 
    
	private boolean checkBoxHit;
    
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
        int index = 0;
        timeUs = new CheckBox( width/2 - widthT/2 - width/16, startY - heightT/2, widthT, heightT, "us", index++ );
        timeS  = new CheckBox( width/2 - widthT/2 + width/16, startY - heightT/2 , widthT, heightT, "h:m:s", index++ );
        
        checkBoxes = new ArrayList<CheckBox>();
        checkBoxes.add( timeUs );
        checkBoxes.add( timeS );
    }
    
    private long roundValue( double value ) {
        return (long) (Math.round( value * 100.0 ) / 100.0);
    }
    
    private void setTime( NetworkDisplay nd ) {
        cursor.setX( Math.max( Math.min( mouseX - widthCursor/2, timing.getMaxX() - widthCursor/2 ), startTimingX - widthCursor/2 ) );
        nd.setTimeSimulation( roundValue( (((double) cursor.getCenterX() - startTimingX) / timing.getWidth() * AnimationNetwork.timeSimulation) ) );
    }
    
    public long getTime( float mouseX ) {
        return roundValue( (((double) Math.max( Math.min( mouseX, timing.getMaxX() ), startTimingX ) - startTimingX) / timing.getWidth() * AnimationNetwork.timeSimulation) );
    }
    
    private void setCursor( int index, NetworkDisplay nd ) {
    	if (index == 0) {
    		timer = Math.min( timer + moving, AnimationNetwork.timeSimulation );
    	} else {
    		timer = Math.max( timer - moving, 0 );
    	}
    	
    	nd.setTimeSimulation( timer );
    }
    
    private void setPositionArrows( float widthS ) {
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
	   			info = info + "00" + ns + "us";
	       	} else if (ns < 100) {
	       		info = info + "0" + ns + "us";
	       	} else {
	       		info = info + ns + "us";
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
	   			info = info + "00" + ns + "us";
	       	} else if (ns < 100) {
	       		info = info + "0" + ns + "us";
	       	} else {
	       		info = info + ns + "us";
	       	}
       	}
       	
       	/*if (gc.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
       	    System.out.println( "NS = " + ns );
       	}*/
       	
    	return info;
    }
    
    private void resetAll() {
    	checkBoxHit = false;
    	arrowHit = false;
    	timingHit = false;
    	
    	for (ArrowButton arrow: arrows) {
    		arrow.setPressed( false );
    	}
    	
    	index = -1;
    }
    
    public void resetIndex() {
    	index = -1;
    }
    
    public boolean checkClick( Event event, NetworkDisplay nd ) {
    	if (index == -1 && !timingHit) {
    		if (event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
    			for (CheckBox box: checkBoxes) {
    				if (box.checkClick( mouseX, mouseY )) {
    					index = box.getIndex();
    					checkBoxHit = true;
    	        		event.setConsumed( true );
    					return true;
    				}
    			}
    			
    			for (ArrowButton arrow: arrows) {
    				if (arrow.contains( mouseX, mouseY )) {
    					index = arrow.getIndex();
    					arrowHit = true;
    					return true;
    				}
    			}
    			
    			if (timing.contains( mouseX, mouseY )) {
    				timingHit = true;
    				return true;
    			}
    		}
    	}
    	
    	return false;
    }
    
    @Override
    public void update( int delta, GameContainer gc, AnimationManager am, Event event, NetworkDisplay nd, boolean mouseEvent )
    {
        mouseX = gc.getInput().getMouseX();
        mouseY = gc.getInput().getMouseY();
        Graphics g = gc.getGraphics();
        
        mouse.setLocation( mouseX, mouseY );
        
        timer = nd.getTimeSimulation();

        // Reset the visibility at the very beginning.
        NetworkDisplay.info.setVisible( false );

        if (index != -1 || timingHit) {
	        if (arrowHit) {
	        	if (!event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
	        		resetAll();
	        	} else {
		        	ArrowButton arrow = arrows.get( index );
		        	if (!arrow.isPressed()) {
		        		arrow.setPressed( true );
		        		setCursor( index, nd );
		        	} else if (++AnimationNetwork.tick >= 50) {
		        		setCursor( index, nd );
		            }
	        	}
	        }
	        
        	if (checkBoxHit) {
        		if (index == 0 && timeS.isSelected()) {
    				timeUs.setSelected();
        		} else if (index == 1 && timeUs.isSelected()) {
            		timeS.setSelected();
        		}
            	
            	if (timeUs.isSelected()) {
            		moving = 1;
            	} else {
            		moving = 1 * limit;
            	}
        		
        		index = -1;
        		checkBoxHit = false;
        	}
        	
        	if (timingHit) {
        		if (!event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
        			resetAll();
        		} else {
        			setTime( nd );
        		}
	        }
        }
        
        if (timing.intersects( mouse ) || timingHit) {
        	String info = setTime( getTime( mouseX ), gc );
        	
            float fontW = g.getFont().getWidth( info );
            float x = Math.max( Math.min( mouseX, timing.getMaxX() ), startTimingX ) - fontW/2;
            x = Math.max( x, 0 ); x = Math.min( x, width - fontW );
            NetworkDisplay.info.setAttributes( g, info, x, timing.getMaxY() + offsetH );
            NetworkDisplay.info.setVisible( true );
        }
        
        cursor.setX( startTimingX - widthCursor/2 + timing.getWidth() / AnimationNetwork.timeSimulation * timer );
    }
    
    @Override
    public void render( GameContainer gc )
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
