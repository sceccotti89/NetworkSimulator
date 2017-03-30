
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
    
    private int frame = 3;
    
    private static final int limit = Integer.MAX_VALUE;
    
    private final String START = "Start", STOP = "Stop", PAUSE = "Pause", PLUS = "Plus", MINUS = "Minus";
    
    private boolean resetButton;
    
    private boolean mouseDown;
    
    public AnimationManager( final GameContainer gc, final float startY ) throws SlickException
    {
        height = gc.getHeight()*10/75;
        width  = gc.getWidth()*10/53;
        
        buttons = new ArrayList<ImageButton>();
        
        resetButton = false;

        start = new ImageButton( 0, startY, width, height, START, Color.gray, 0, gc, new Image( "./data/Image/Start.png" ), gc.getWidth()/20, gc.getHeight()/20 );
        pause = new ImageButton( start.getMaxX(), startY, width, height, PAUSE, Color.gray, 1, gc, new Image( "./data/Image/Pause.png" ), gc.getWidth()/20, gc.getHeight()/20 );
        stop  = new ImageButton( pause.getMaxX(), startY, width, height, STOP, Color.gray, 2, gc, new Image( "./data/Image/Stop.png" ), gc.getWidth()/20, gc.getHeight()/20 );
        minus = new ImageButton( stop.getMaxX() + gc.getWidth()/15, startY + gc.getHeight()/20, gc.getWidth()/20, gc.getHeight()/20, MINUS, Color.yellow, 3, gc, new Image( "./data/Image/Minus.png" ), gc.getWidth()/45, gc.getHeight()/70 );
        plus  = new ImageButton( stop.getMaxX() + gc.getWidth()/4, startY + gc.getHeight()/20, gc.getWidth()/20, gc.getHeight()/20, PLUS, Color.yellow, 4, gc, new Image( "./data/Image/Plus.png" ), gc.getWidth()/40, gc.getHeight()/30 );
        
        speed     = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        showFrame = new Rectangle( minus.getMaxX() + (plus.getX() - minus.getMaxX())/2 - gc.getWidth()/22, minus.getY() + gc.getHeight()/270, gc.getWidth()/10*String.valueOf( frame ).length()/2 + gc.getWidth()/20, gc.getHeight()/20 );
        
        buttons.add( start );
        buttons.add( stop );
        buttons.add( pause );
        buttons.add( plus );
        buttons.add( minus );
    }
	
	private int checkButton( ImageButton button, Input input, int i ) {
		if (button.isPressed()) {
			return 1;
		}
	
		return 0;
	}
    
    @Override
    public void update( final GameContainer gc, final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta, final NetworkDisplay nd )
    {    	
    	mouseX = input.getMouseX();
		mouseY = input.getMouseY();
		
		if (plus.isPressed()) {
			plus.setPressed();
		} else if (minus.isPressed()) {
			minus.setPressed();
		}
		
		if (leftMouse) {
			if (!mouseDown) {
	            mouseDown = true;
	            
	            for (ImageButton button : buttons) {
	                if (button.checkClick( mouseX, mouseY, input )) {
	                	if (!button.isPressed()) {
	                		button.setPressed();
	                	}
	                }
	            }
            }
		} else {
			if (mouseDown) {
                mouseDown = false;
                
                for (ImageButton button: buttons) {
            		int value = checkButton( button, input, button.getIndex() );
                	// se e' stato premuto il tasto
            		if (value > 0) {
                        for (ImageButton bottone: buttons) {
                        	if (bottone.isPressed()) {
                        		bottone.setPressed();
                        	}
                        }
                        // pressed tramite mouse
                        if (button.checkClick( mouseX, mouseY, input )) {
                        	if (button.getName().equals( PLUS )) {
        						frame = Math.min( limit, frame + 5 );
        					} else if (button.getName().equals( MINUS )) {
        						frame = Math.max( 0, frame - 5 );
        					} else if (button.getName().equals( START )) {
        						resetButton = nd.startAnimation();
        						ob.resetAllButtons();
        					} else if (button.getName().equals( PAUSE )) {
        						resetButton = nd.pauseAnimation();
        						ob.resetAllButtons();
        					} else if (button.getName().equals( STOP )) {
        						resetButton = nd.stopAnimation();
        						ob.resetAllButtons();
        					}
                        }
        			}
            	}
            }
		}
		
		/*if (leftMouse) {			
			resetButton = false;
			for (ImageButton button: buttons) {
				if (button.checkClick( mouseX, mouseY )) {
					if (button.getName().equals( PLUS )) {
						frame = Math.min( limit, frame + 5 );
					} else if (button.getName().equals( MINUS )) {
						frame = Math.max( 0, frame - 5 );
					} else if (button.getName().equals( START )) {
						resetButton = nd.startAnimation();
						ob.resetAllButtons();
					} else if (button.getName().equals( PAUSE )) {
						resetButton = nd.pauseAnimation();
						ob.resetAllButtons();
					} else if (button.getName().equals( STOP )) {
						resetButton = nd.stopAnimation();
						ob.resetAllButtons();
					}
					
					if (resetButton) {
						for (ImageButton obj: buttons) {
							if (obj != button && obj.isPressed()) {
								obj.setPressed();
							}
						}
					}
					
					button.setPressed();
				}
			}
		}*/
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
    
    public void resetAllButtons()
    {
    	for(ImageButton button: buttons){
    		if(button.isPressed())
    			button.setPressed();
    	}
    }
    
    public int getFrames() {
    	return frame;
    }
}