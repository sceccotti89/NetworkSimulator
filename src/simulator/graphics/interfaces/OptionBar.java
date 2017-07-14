
package simulator.graphics.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;

import simulator.graphics.dataButton.SimpleButton;
import simulator.graphics.elements.Operation;

public class OptionBar implements AnimationInterface
{
    private Rectangle barOptions = new Rectangle( 0, 0, 800, 20 );
    
    private SimpleButton file, options, edit;
    private Operation move, add, remove;
    private Operation client, server, switcher, packet;
    
    private float widthB, heightB;
    
    private List<SimpleButton> buttons;
    private List<Operation> operation, type;

    private int mouseX, mouseY;
    
    private String FILE = "File", OPTIONS = "Options", EDIT = "Edit",
                   MOVE = "MoveNode", ADD = "AddElement", REMOVE = "RemoveNode",
                   CLIENT = "Client", SERVER = "Server", SWITCH = "Switch", PACKET = "Packet";
    
    private boolean mouseDown;
    
    private boolean chooseOption = false, chooseType = false;
    
    private Shape[] areaType;
    
    public OptionBar( final GameContainer gc, final int width, final int height ) throws SlickException
    {
        widthB = width/10;
        heightB = height/30;
        
        buttons = new ArrayList<SimpleButton>();
        
        int index = 0;
        file    = new SimpleButton( 0, 0, widthB, heightB, FILE, Color.gray, index++, gc );
        options = new SimpleButton( file.getMaxX(), 0, widthB, heightB, OPTIONS, Color.gray, index++, gc );
        edit    = new SimpleButton( options.getMaxX(), 0, widthB, heightB, EDIT, Color.gray, index++, gc );
        
        buttons.add( file );
        buttons.add( options );
        buttons.add( edit );
        
        operation = new ArrayList<Operation>();
        
        float startX = edit.getX() + width/400;
        move   = new Operation( MOVE, startX, edit.getMaxY(), widthB, heightB );
        add    = new Operation( ADD, startX, move.getMaxY(), widthB, heightB );
        remove = new Operation( REMOVE, startX, add.getMaxY(), widthB, heightB );
        
        operation.add( move );
        operation.add( add );
        operation.add( remove );
        
        type = new ArrayList<Operation>();
        
        startX = move.getMaxX();
        client   = new Operation( CLIENT, startX, add.getY(), widthB, heightB );
        server   = new Operation( SERVER, startX, client.getMaxY(), widthB, heightB );
        switcher = new Operation( SWITCH, startX, server.getMaxY(), widthB, heightB );
        packet   = new Operation( PACKET, startX, switcher.getMaxY(), widthB, heightB );
        
        type.add( client );
        type.add( server );
        type.add( switcher );
        type.add( packet );
        
        areaType = new Shape[] {add.getArea()};
        for (Operation op: type) {
            areaType = areaType[0].union( op.getArea() );
        }
    }
    
    public float getMaxY() {
        return barOptions.getMaxY();
    }
    
    @Override
    public void update( final int delta, final GameContainer gc, final boolean leftMouse, final NetworkDisplay nd ) throws SlickException
    {
        mouseX = gc.getInput().getMouseX();
        mouseY = gc.getInput().getMouseY();
        
        if (chooseOption) {
            chooseType = areaType[0].contains( mouseX, mouseY );
        }
        
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
            
            // TODO SETTARE DELLE DIPENDENZE (UNA OPERAZIONE ALLA VOLTA E' CONSENTITA)
            for (Operation op : operation) {
            	if (op.checkCollision( mouseX, mouseY )) {
                    if (chooseOption) {
		            	if (op.getName().equals( MOVE )) {
	                        nd.setNodeSelectable();
	                    } else if (op.getName().equals( REMOVE )) {
	                        nd.removeNode();
	                    } else {
	                        break;
	                    }
                    }
	            	
	            	chooseOption = false;
            	}
            }
            
            if (chooseType) {
                if (!nd.isInExecution()) {
                    for (Operation op: type) {
                        if (op.checkCollision( mouseX, mouseY )) {
                            if (op.getName().equals( CLIENT )) {
                                nd.addClient( mouseX, mouseY );
                            } else if (op.getName().equals( SERVER )) {
                                nd.addServer( mouseX, mouseY );
                            } else if (op.getName().equals( SWITCH )) {
                                nd.addSwitch( mouseX, mouseY );
                            } else if (op.getName().equals( PACKET )) {
                                nd.addPacket( mouseX, mouseY );
                            }
                            
                            chooseOption = false;
                            chooseType = false;
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
        	for (Operation op: operation) {
        		op.render( g );
        	}
        	
        	if (chooseType) {
        	    for(Operation op: type) {
        	        op.render( g );
        	    }
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
