
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
    private Rectangle play, stop, pause, fastSlow;
    private float width, height;
    
    private ArrayList<SimpleButton> buttons;
    private SimpleButton start, fermo, pausa, speed;
    
    private int mouseX, mouseY;
    
    public AnimationManager( final GameContainer gc, final float startY ) throws SlickException
    {
        height = gc.getHeight()*10/75;
        width = gc.getWidth()*10/53;
        
        buttons = new ArrayList<SimpleButton>();
        
        play     = new Rectangle( 0, startY, width, height );
        stop     = new Rectangle( play.getMaxX(), startY, width, height );
        pause    = new Rectangle( stop.getMaxX(), startY, width, height );
        fastSlow = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        
        // TODO COMPLETARE I VARI BOTTONI        
        start = new SimpleButton( 0, startY, width, height, "START", Color.gray, 0, gc );
        fermo = new SimpleButton( start.getMaxX(), startY, width, height, "STOP", Color.gray, 1, gc );
        pausa = new SimpleButton( fermo.getMaxX(), startY, width, height, "PAUSE", Color.gray, 2, gc );
        speed = new SimpleButton( pausa.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height, "SPEED", Color.gray, 3, gc );
        
        buttons.add( start );
        buttons.add( fermo );
        buttons.add( pausa );
        buttons.add( speed );
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
    public void render( final Graphics g )
    {
        g.draw( play );
        g.draw( stop );
        g.draw( pause );
        g.draw( fastSlow );
        
        start.draw( g );
        fermo.draw( g );
        pausa.draw( g );
        speed.draw( g );
    }
    
    public void resetAllButtons()
    {
    	for(SimpleButton button: buttons){
    		if(button.isPressed())
    			button.setPressed();
    	}
    }
}