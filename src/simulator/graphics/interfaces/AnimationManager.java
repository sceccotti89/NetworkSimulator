
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
    
    private Rectangle speed, showFrame;
    
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
        minus = new SimpleButton( pause.getMaxX() + gc.getWidth()/15, startY + gc.getHeight()/20, gc.getWidth()/20, gc.getHeight()/20, MINUS, Color.yellow, 3, gc );
        plus = new SimpleButton( pause.getMaxX() + gc.getWidth()/4, startY + gc.getHeight()/20, gc.getWidth()/20, gc.getHeight()/20, PLUS, Color.yellow, 4, gc );
        
        speed     = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        showFrame = new Rectangle( minus.getMaxX() + (plus.getX() - minus.getMaxX())/2 - gc.getWidth()/22, minus.getY() + gc.getHeight()/270, gc.getWidth()/10*String.valueOf( frame ).length()/2 + gc.getWidth()/20, gc.getHeight()/20 );
        
        buttons.add( start );
        buttons.add( stop );
        buttons.add( pause );
        buttons.add( plus );
        buttons.add( minus );
    }
    
    public void setLenghtShowFrame( GameContainer gc ){
    	showFrame.setWidth( String.valueOf( frame ).length()*gc.getWidth()/10 + gc.getWidth()/20 );
    }
    
    @Override
    public void update( final GameContainer gc, final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta, final NetworkDisplay nd )
    {    	
    	mouseX = input.getMouseX();
		mouseY = input.getMouseY();
		
		if(plus.isPressed())
			plus.setPressed();
		else if(minus.isPressed())
			minus.setPressed();
		
		if(leftMouse){
			for(SimpleButton button: buttons){
				if(button.checkClick( mouseX, mouseY )){
					ob.resetAllButtons();
					if(button.getName().equals( PLUS )){
						frame = Math.min( 100, frame + 10 );
						setLenghtShowFrame( gc );
					}
					else if(button.getName().equals( MINUS )){
						frame = Math.max( 0, frame - 10 );
						setLenghtShowFrame( gc );
					}
					else if(button.getName().equals( START )){
						nd.startAnimation();
					}
					else if(button.getName().equals( PAUSE )){
						nd.pauseAnimation();
					}
					else if(button.getName().equals( STOP )){
						nd.stopAnimation();
					}
					for(SimpleButton obj: buttons){
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
    public void render( final GameContainer gc )
    {    	
    	Graphics g = gc.getGraphics();
    	
    	g.setColor( Color.gray );
    	g.fill( speed );
    	
    	for(SimpleButton button: buttons){
    		button.draw( g );
    	}
    	
    	g.setColor( Color.black );
    	g.draw( showFrame );
    	g.drawString( String.valueOf( frame ), plus.getMaxX() + (minus.getX() - plus.getMaxX())/2 - String.valueOf( frame ).length()/2*gc.getWidth()/80, plus.getY() + (plus.getMaxY() - plus.getY())/4 );
    }
    
    public void resetAllButtons()
    {
    	for(SimpleButton button: buttons){
    		if(button.isPressed())
    			button.setPressed();
    	}
    }
}