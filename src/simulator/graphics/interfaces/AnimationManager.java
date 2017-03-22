
package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.SimpleButton;

public class AnimationManager implements AnimationInterface
{
    private float width, height;
    
    private Rectangle speed;
    
    private ArrayList<SimpleButton> buttons;
    private SimpleButton start, stop, pause, plus, minus;
    
    private int mouseX, mouseY;
    
    private int frame = 0;
    
    private String START = "Start", STOP = "Stop", PAUSE = "Pause", PLUS = "Plus", MINUS = "Minus";
    
    public AnimationManager( final GameContainer gc, final float startY ) throws SlickException
    {
        height = gc.getHeight()*10/75;
        width  = gc.getWidth()*10/53;
        
        buttons = new ArrayList<SimpleButton>();
              
        start = new SimpleButton( 0, startY, width, height, START, Color.gray, 0, gc );
        stop = new SimpleButton( start.getMaxX(), startY, width, height, STOP, Color.gray, 1, gc );
        pause = new SimpleButton( stop.getMaxX(), startY, width, height, PAUSE, Color.gray, 2, gc );
        plus = new SimpleButton( pause.getMaxX() + gc.getWidth()/15, startY + gc.getHeight()/20, gc.getWidth()/20, gc.getHeight()/20, PLUS, Color.yellow, 3, gc );
        minus = new SimpleButton( pause.getMaxX() + gc.getWidth()/4, startY + gc.getHeight()/20, gc.getWidth()/20, gc.getHeight()/20, MINUS, Color.yellow, 4, gc );
        
        speed = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        
        buttons.add( start );
        buttons.add( stop );
        buttons.add( pause );
        buttons.add( plus );
        buttons.add( minus );
    }
    
    @Override
    public void update( final GameContainer gc, final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta)
    {    	
    	mouseX = input.getMouseX();
		mouseY = input.getMouseY();
		
		if(leftMouse){
			for(SimpleButton button: buttons){
				if(button.checkClick( mouseX, mouseY )){
					ob.resetAllButtons();
					if(button.getName().equals( PLUS )){
						frame = Math.min( 100, frame + 10 );
					}
					else if(button.getName().equals( MINUS )){
						frame = Math.max( 0, frame - 10 );
					}
					else for(SimpleButton obj: buttons){
							if(obj != button && obj.isPressed()){
								obj.setPressed();
							}
						}
					
					button.setPressed();
				}
			}
		}
    }
    
    @Override
    public void render( final Graphics g )
    {    	
    	g.setColor( Color.gray );
    	g.fill( speed );
    	
    	for(SimpleButton button: buttons){
    		button.draw( g );
    	}
    }
    
    public void resetAllButtons()
    {
    	for(SimpleButton button: buttons){
    		if(button.isPressed())
    			button.setPressed();
    	}
    }
}