
package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.AnimationNetwork;
import simulator.graphics.dataButton.ImageButton;
import simulator.graphics.elements.Event;

public class AnimationManager implements AnimationInterface
{
    private float width, height;
    
    private Rectangle speed, showFrame;
    
    private ArrayList<ImageButton> buttons;
    private ImageButton start, stop, pause, plus, minus;
    
    private int mouseX, mouseY;
    private float startY;
    
    private int index = -1;
    
    public static int frames = 1;
    
    //private int timer = 0;
    
    private static final int limit = Integer.MAX_VALUE;
    
    private final String START = "Start", STOP = "Stop", PAUSE = "Pause", PLUS = "Plus", MINUS = "Minus";
    
    private String frameLenght = "" + frames;
    
    public AnimationManager( GameContainer gc, float startY, float widthM, float heightM ) throws SlickException
    {
    	this.startY = startY;
    	
        height = heightM*10/95;
        width  = widthM*10/53;
        float widthFrame = widthM/10;
        
        buttons = new ArrayList<ImageButton>();

        int index = 0;
        start = new ImageButton( 0, startY, width, height, START, Color.gray, index++, gc, new Image( "./data/Image/Start.png" ), widthM/20, heightM/20 );
        pause = new ImageButton( start.getMaxX(), startY, width, height, PAUSE, Color.gray, index++, gc, new Image( "./data/Image/Pause.png" ), widthM/20, heightM/20 );
        stop  = new ImageButton( pause.getMaxX(), startY, width, height, STOP, Color.gray, index++, gc, new Image( "./data/Image/Stop.png" ), widthM/20, heightM/20 );
        minus = new ImageButton( stop.getMaxX() + widthM/15, startY + height/2 - heightM/40, widthM/20, heightM/20, MINUS, Color.yellow, index++, gc, new Image( "./data/Image/Minus.png" ), widthM/45, heightM/90 );
        plus  = new ImageButton( stop.getMaxX() + widthM/4, startY + height/2 - heightM/40, widthM/20, heightM/20, PLUS, Color.yellow, index++, gc, new Image( "./data/Image/Plus.png" ), widthM/40, heightM/30 );

        showFrame = new Rectangle( minus.getMaxX() + (plus.getX() - minus.getMaxX())/2 - widthFrame/2, minus.getY(), widthFrame, minus.getAlt());
        speed     = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        
        buttons.add( start );
        buttons.add( pause );
        buttons.add( stop );
        buttons.add( minus );
        buttons.add( plus );
    }
    
    private void setFrames( int index, NetworkDisplay nd ) {
        int add;
        if (index == 4) {
            add = 1;
        } else {
            add = -1;
        }
        
        frames = Math.min( limit, Math.max( frames + add, 1 ) );
        nd.setPacketSpeed();
    }
    
    public float getStartY() {
    	return startY;
    }
    
    public void resetIndex() {
    	index = -1;
    }
    
    private void resetAll() {
    	for (ImageButton button: buttons) {
    		button.setPressed( false );
    	}
    	
    	index = -1;
    }
    
    public boolean checkClick( Event event, NetworkDisplay nd ) {
    	if (event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
	    	if (index == -1) {
	    		for (ImageButton button : buttons) {
	                if (button.checkClick( mouseX, mouseY ) && !button.isPressed()) {
	                	index = button.getIndex();
	                	if (!button.getName().equals( PLUS ) && !button.getName().equals( MINUS )) {
	                		event.setConsumed( true );
	                	}
	                
	                	return true;
	                }
	    		}
	    	} else { 
	    		return true;
	    	}
    	}
    	
    	return false;
    }
    
    @Override
    public void update( int delta, GameContainer gc, AnimationManager am, Event event, NetworkDisplay nd, boolean mouseEvent )
    {        
        mouseX = gc.getInput().getMouseX();
        mouseY = gc.getInput().getMouseY();
        
        if (index >= 0 && (buttons.get( index ).getName().equals( PLUS ) || buttons.get( index ).getName().equals( MINUS ))) {
        	if (!event.getInput().isMouseButtonDown( Input.MOUSE_LEFT_BUTTON )) {
        		resetAll();
        		return;
        	}
        	
            ImageButton button = buttons.get( index );
            if (button.contains( mouseX, mouseY )) {
            	if (!button.isPressed()) {
            		button.setPressed( true );
                	setFrames( index = button.getIndex(), nd );
            	} else if (++AnimationNetwork.tick >= 50) {
                    setFrames( index, nd );
                }
            }
        }
        
        if (index != -1) {
            ImageButton button = buttons.get( index );
        	if (button.getName().equals( START )) {
                nd.startAnimation();
                button.setPressed( true );
                resetButtons( button );
	            index = -1;
            } else if (button.getName().equals( PAUSE )) {
            	if (nd.isInExecution()) {
            		nd.pauseAnimation();
                    button.setPressed( true );
                    resetButtons( button );
    	            index = -1;
            	} else {
            		button.setPressed( false );
            		index = -1;
            	}
            } else if (button.getName().equals( STOP )) {
                nd.stopAnimation();
                resetButtons( null );
                button.setPressed( false );
	            index = -1;
            }
        	
        	//event.setConsumed( true );
        }
    }
    
    public float getMaxY() {
        return start.getMaxY();
    }
    
    @Override
    public void render( GameContainer gc )
    {        
        Graphics g = gc.getGraphics();
        
        g.setColor( Color.gray );
        g.fill( speed );
        
        for (ImageButton button: buttons) {
            button.draw( g );
        }
        
        g.setColor( Color.white );
        g.fill( showFrame );
        g.setColor( Color.black );
        g.draw( showFrame );
        
        frameLenght = "" + frames;
        int fWidth = g.getFont().getWidth( frameLenght ), fHeight = g.getFont().getHeight( frameLenght );
        g.drawString( frameLenght, showFrame.getCenterX() - fWidth/2, showFrame.getCenterY() - fHeight/2 );
    }
    
    public void resetAllButtons() {
        for (ImageButton button: buttons) {
            if (button.isPressed()) {
                button.setPressed( false );
            }
        }
    }
    
    private void resetButtons( ImageButton button ) {
        for (ImageButton imButton: buttons) {
            if (imButton != button) {
                imButton.setPressed( false );
            }
        }
    }
}
