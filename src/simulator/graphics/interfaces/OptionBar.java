
package simulator.graphics.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.SimpleButton;
import simulator.graphics.elements.Operation;

public class OptionBar implements AnimationInterface
{
    private Rectangle barOptions = new Rectangle( 0, 0, 800, 20 );
    
    private SimpleButton file, options, edit;
    private Operation move, add, remove;
    
    private float width, height;
    
    private List<SimpleButton> buttons;
    private List<Operation> operation;

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
        
        buttons.add( file );
        buttons.add( options );
        buttons.add( edit );
        
        operation = new ArrayList<Operation>();
        
        move   = new Operation( MOVE, edit.getX(), edit.getMaxY(), width, height );
        add    = new Operation( ADD, move.getX(), move.getMaxY(), width, height );
        remove = new Operation( REMOVE, add.getX(), add.getMaxY(), width, height );
        
        operation.add( move );
        operation.add( add );
        operation.add( remove );
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
                        }
                    }
                }
            }
            
            // TODO INSERIRE I NOMI DELLE OPERAZIONI SUI BOTTONI
            if (chooseOption) {
	            for (Operation op : operation) {
	            	if (op.checkCollision( mouseX, mouseY )) {
		            	if (op.getName().equals( MOVE )) {
	                        nd.setNodeSelectable();
	                    } else if (op.getName().equals( ADD )) {
	                    	if (!nd.isInExecution()) {
	                    		nd.addNewNode( mouseX, mouseY );
	                    	}
	                    } else if (op.getName().equals( REMOVE )) {
	                        nd.removeNode();
	                    }
		            	
		            	chooseOption = false;
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
        	for (Operation op: operation) {
        		op.render( g );
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
