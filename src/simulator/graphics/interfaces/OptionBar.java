
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
    
    private SimpleButton file, options, edit, node;
    
    private float width, height;
    
    private ArrayList<SimpleButton> buttons;

    private int mouseX, mouseY;
    
    private String FILE = "File", OPTIONS = "Options", EDIT = "Edit", NODE = "NewNode";
    
    private boolean mouseDown;
    
    public OptionBar( final GameContainer gc ) throws SlickException
    {
        width  = gc.getWidth()/10;
        height = gc.getHeight()/30;
        
        buttons = new ArrayList<SimpleButton>();
        
        int index = 0;
        file    = new SimpleButton( 0, 0, width, height, FILE, Color.gray, index++, gc );
        options = new SimpleButton( file.getMaxX(), 0, width, height, OPTIONS, Color.gray, index++, gc );
        edit    = new SimpleButton( options.getMaxX(), 0, width, height, EDIT, Color.gray, index++, gc );
        node    = new SimpleButton( edit.getMaxX(), 0, width, height, NODE, Color.gray, index++, gc );
        
        buttons.add( file );
        buttons.add( options );
        buttons.add( edit );
        buttons.add( node );
    }
    
    public float getMaxY() {
        return barOptions.getMaxY();
    }
    
    @Override
    public void update( final int delta, final GameContainer gc, final boolean leftMouse, final NetworkDisplay nd ) throws SlickException
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
                        } else if (button.getName().equals( EDIT )) {
                            nd.setNodeSelectable();
                        } else if (button.getName().equals( NODE )) {
                        	if (!nd.isInExecution()) {
                        		nd.addNewNode( mouseX, mouseY );
                        		gc.getInput().clearKeyPressedRecord();
                        	}
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
