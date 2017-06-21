
package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.ImageButton;

public class AnimationManager implements AnimationInterface
{
    private float width, height;
    
    private Rectangle speed, showFrame;
    
    private ArrayList<ImageButton> buttons;
    private ImageButton start, stop, pause, plus, minus;
    
    private int mouseX, mouseY;
    
    private int frame = 1;
    
    private static final int limit = Integer.MAX_VALUE;
    
    private final String START = "Start", STOP = "Stop", PAUSE = "Pause", PLUS = "Plus", MINUS = "Minus";
    
    private boolean mouseDown;
    
    public AnimationManager( final GameContainer gc, final float startY, final float widthM, final float heightM ) throws SlickException
    {
        height = heightM*10/75;
        width  = widthM*10/53;
        
        buttons = new ArrayList<ImageButton>();

        start = new ImageButton( 0, startY, width, height, START, Color.gray, 0, gc, new Image( "./data/Image/Start.png" ), widthM/20, heightM/20 );
        pause = new ImageButton( start.getMaxX(), startY, width, height, PAUSE, Color.gray, 1, gc, new Image( "./data/Image/Pause.png" ), widthM/20, heightM/20 );
        stop  = new ImageButton( pause.getMaxX(), startY, width, height, STOP, Color.gray, 2, gc, new Image( "./data/Image/Stop.png" ), widthM/20, heightM/20 );
        minus = new ImageButton( stop.getMaxX() + widthM/15, startY + heightM/20, widthM/20, heightM/20, MINUS, Color.yellow, 3, gc, new Image( "./data/Image/Minus.png" ), widthM/45, heightM/70 );
        plus  = new ImageButton( stop.getMaxX() + widthM/4, startY + heightM/20, widthM/20, heightM/20, PLUS, Color.yellow, 4, gc, new Image( "./data/Image/Plus.png" ), widthM/40, heightM/30 );
        
        speed     = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        showFrame = new Rectangle( minus.getMaxX() + (plus.getX() - minus.getMaxX())/2 - gc.getWidth()/22, minus.getY() + gc.getHeight()/270, gc.getWidth()/10*String.valueOf( frame ).length()/2 + gc.getWidth()/20, gc.getHeight()/20 );
        
        buttons.add( start );
        buttons.add( stop );
        buttons.add( pause );
        buttons.add( plus );
        buttons.add( minus );
    }
    
    @Override
    public void update( final int delta, final Input input, final boolean leftMouse, OptionBar ob, AnimationManager am, TimeAnimation ta, NetworkDisplay nd )
    {    	
    	mouseX = input.getMouseX();
		mouseY = input.getMouseY();
		
		if (leftMouse && !mouseDown) {
            mouseDown = true;
            
            for (ImageButton button : buttons) {
                if (button.checkClick( mouseX, mouseY ) && !button.isPressed()) {
            		button.setPressed( true );
            	}
            }
		} else if (!leftMouse && mouseDown) {
            mouseDown = false;
            
            boolean buttonFounded = false;
			for (ImageButton button: buttons) {
            	if (button.checkClick( mouseX, mouseY )) {
            		buttonFounded = true;
                	if (button.getName().equals( PLUS )) {
						frame = Math.min( limit, frame + 5 );
					} else if (button.getName().equals( MINUS )) {
						frame = Math.max( 1, frame - 5 );
					} else if (button.getName().equals( START )) {
						nd.startAnimation();
						ob.resetAllButtons();
						button.setPressed( true );
						resetButtons( button );
					} else if (button.getName().equals( PAUSE )) {
						nd.pauseAnimation();
						ob.resetAllButtons();
						resetButtons( button );
					} else if (button.getName().equals( STOP )) {
						nd.stopAnimation();
						ob.resetAllButtons();
						resetButtons( null );
					}
                }
        	}
            
            if (!buttonFounded){
            	for (ImageButton bottone: buttons) {
                	if (bottone.isPressed()) {
                		if ((bottone.getName().equals( START ) && !nd.isOperating())
                		 || (bottone.getName().equals( PAUSE ) && !nd.isInPause())
                		 || (bottone.getName().equals( STOP ))) {
                			bottone.setPressed( false );
                		}
                	}
                }
            }
        }
	}
    
    public float getMaxY() {
    	return start.getMaxY();
    }
    
    @Override
    public void render( final GameContainer gc )
    {    	
    	Graphics g = gc.getGraphics();
    	
    	g.setColor( Color.gray );
    	g.fill( speed );
    	
    	for (ImageButton button: buttons) {
    		button.draw( g );
    	}
    	
    	g.setColor( Color.black );
    	g.draw( showFrame );
    	g.drawString( String.valueOf( frame ), plus.getMaxX() + (minus.getX() - plus.getMaxX())/2 - String.valueOf( frame ).length()/2*gc.getWidth()/80, plus.getY() + (plus.getMaxY() - plus.getY())/4 );
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
    
    public int getFrames() {
    	return frame;
    }
}