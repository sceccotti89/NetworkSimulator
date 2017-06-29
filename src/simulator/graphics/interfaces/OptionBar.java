
package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
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
    
    private String FILE = "File", OPTIONS = "Options";
    
    private boolean mouseDown;
    
    public OptionBar( final GameContainer gc ) throws SlickException
    {
        width  = gc.getWidth()/10;
        height = gc.getHeight()/30;
        
        buttons = new ArrayList<SimpleButton>();
        
        file    = new SimpleButton( 0, 0, width, height, FILE, Color.gray, 0, gc );
        options = new SimpleButton( file.getMaxX(), 0, width, height, OPTIONS, Color.gray, 1, gc );
        
        buttons.add( file );
        buttons.add( options );
    }
    
    public float getMaxY() {
        return barOptions.getMaxY();
    }
    
    @Override
    public void update( final int delta, final GameContainer gc, final boolean leftMouse, final OptionBar ob, final NetworkDisplay nd )
    {
        mouseX = gc.getInput().getMouseX();
        mouseY = gc.getInput().getMouseY();
        
        if (leftMouse && !mouseDown) {
            mouseDown = true;
            
            for (SimpleButton button : buttons) {
                if (button.checkClick( mouseX, mouseY ) && !button.isPressed()) {
                    button.setPressed( true );
                }
            }
        } else if (!leftMouse && mouseDown) {
            mouseDown = false;
            
            for (SimpleButton button: buttons) {
                // if a button is pressed
                if (button.isPressed()) {
                    for (SimpleButton bottone: buttons) {
                        if (bottone.isPressed()) {
                            bottone.setPressed( false );
                        }
                    }

                    if (button.checkClick( mouseX, mouseY )) {
                        if (button.getName().equals( FILE )) {
                            ;
                        } else if (button.getName().equals( OPTIONS )) {
                            ;
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void render( final GameContainer gc ) {
        Graphics g = gc.getGraphics();
        
        for (SimpleButton button: buttons) {
            button.draw( g );
        }
    }
    
    public void resetAllButtons() {
        for (SimpleButton button: buttons) {
            if (button.isPressed()) {
                button.setPressed( false );
            }
        }
    }
}
