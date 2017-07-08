
package simulator.graphics.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.SimpleButton;

public class OptionBar implements AnimationInterface
{
    private Rectangle barOptions = new Rectangle( 0, 0, 800, 20 );
    
    private SimpleButton file, options, edit, move, node, remove;
    private Rectangle muovi, adda, rimuovi;
    
    private float width, height;
    
    private List<SimpleButton> buttons;
    private List<Rectangle> operation;

    private int mouseX, mouseY;
    
    private String FILE = "File", OPTIONS = "Options", EDIT = "Edit", MOVE = "MoveNode", ADD = "AddNode", REMOVE = "RemoveNode";
    
    private boolean mouseDown;
    
    private boolean chooseOption = false;
    
    public OptionBar( final GameContainer gc ) throws SlickException
    {
        width  = gc.getWidth()/10;
        height = gc.getHeight()/30;
        
        buttons = new ArrayList<SimpleButton>();
        
        int index = 0;
        file    = new SimpleButton( 0, 0, width, height, FILE, Color.gray, index++, gc );
        options = new SimpleButton( file.getMaxX(), 0, width, height, OPTIONS, Color.gray, index++, gc );
        edit    = new SimpleButton( options.getMaxX(), 0, width, height, EDIT, Color.gray, index++, gc );
        move    = new SimpleButton( edit.getMaxX(), 0, width, height, MOVE, Color.gray, index++, gc );
        node    = new SimpleButton( move.getMaxX(), 0, width, height, ADD, Color.gray, index++, gc );
        remove  = new SimpleButton( node.getMaxX(), 0, width, height, REMOVE, Color.gray, index++, gc );
        
        buttons.add( file );
        buttons.add( options );
        buttons.add( edit );
        buttons.add( move );
        buttons.add( node );
        buttons.add( remove );
        
        operation = new ArrayList<Rectangle>();
        
        muovi   = new Rectangle( edit.getX(), edit.getMaxY(), width, height );
        adda    = new Rectangle( muovi.getX(), edit.getMaxY(), width, height );
        rimuovi = new Rectangle( adda.getX(), edit.getMaxY(), width, height );
        
        operation.add( muovi );
        operation.add( adda );
        operation.add( rimuovi );
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
                        	chooseOption = !chooseOption;
                        } else if (button.getName().equals( MOVE )) {
                            nd.setNodeSelectable();
                        } else if (button.getName().equals( ADD )) {
                        	if (!nd.isInExecution()) {
                        		nd.addNewNode( mouseX, mouseY );
                        	}
                        } else if (button.getName().equals( REMOVE )) {
                            nd.removeNode();
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
        
        if (chooseOption) {
        	for (Rectangle rect: operation) {
        		gc.getGraphics().draw( rect );
        	}
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
