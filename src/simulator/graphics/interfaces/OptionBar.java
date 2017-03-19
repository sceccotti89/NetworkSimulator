
package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.SimpleButton;

public class OptionBar implements AnimationInterface
{
    private Rectangle barOptions = new Rectangle( 0, 0, 800, 20 );
    
    private SimpleButton file, options;
    
    private float width, height;
    
    private ArrayList<SimpleButton> buttons;
    
    private int mouseX, mouseY;
    
    public OptionBar( GameContainer gc ) throws SlickException
	{
    	width  = gc.getWidth()/10;
    	height = gc.getHeight()/30;
    	
    	buttons = new ArrayList<SimpleButton>();
    	
		file    = new SimpleButton( 0, 0, width, height, "File", Color.gray, 0, gc );
		options = new SimpleButton( file.getMaxX(), 0, width, height, "Options", Color.gray, 1, gc );
		
		buttons.add( file );
		buttons.add( options );
	}
    
    public float getMaxY() {
        return barOptions.getMaxY();
    }
    
    @Override
    public void update( final GameContainer gc, final int delta, final Input input, final boolean leftMouse, final OptionBar ob, final AnimationManager am, final TimeAnimation ta )
    {
    	mouseX = input.getMouseX();
		mouseY = input.getMouseY();
    	
    	if(leftMouse){
			for(SimpleButton button: buttons){
				if(button.checkClick( mouseX, mouseY )){
					am.resetAllButtons();
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
        g.draw( barOptions );
        
        file.draw( g );
        options.draw( g );
    }
    
    public void resetAllButtons()
    {
    	for(SimpleButton button: buttons){
    		if(button.isPressed())
    			button.setPressed();
    	}
    }
}