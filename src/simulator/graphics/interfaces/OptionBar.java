
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
import simulator.graphics.dataButton.MenuItem;
import simulator.graphics.elements.Operation;

public class OptionBar implements AnimationInterface
{
    private Rectangle barOptions = new Rectangle( 0, 0, 800, 20 );
    
    
    
    // TODO REALIZZARE I BOTTONI A TENDINA TRAMITE CLASSE
    private /*final*/ MenuItem FILES, OPTION, EDITING;
    private final List<MenuItem> items;
    
    
    private final SimpleButton file, options, edit;
    private final Operation save, load;
    private final Operation move, add, remove;
    private Operation client, server, switcher, packet;
    
    private float widthB, heightB;
    
    private List<SimpleButton> buttons;
    private List<Operation> operation, type, saveload;

    private int mouseX, mouseY;
    
    private String FILE = "File", OPTIONS = "Options", EDIT = "Edit",
    			   SAVE = "Save File", LOAD = "Load File",
                   MOVE = "MoveNode", ADD = "AddElement", REMOVE = "RemoveNode",
                   CLIENT = "Client", SERVER = "Server", SWITCH = "Switch", PACKET = "Packet";
    
    private boolean mouseDown;
    
    private boolean editing = false, chooseType = false, opFile = false;
    
    private Shape[] areaType;



	private ArrayList<Operation> operations;
    
    public OptionBar( final GameContainer gc, final int width, final int height ) throws SlickException
    {
        widthB = width/10;
        heightB = height/30;
        
        int index = 0;
        
        
        // TODO INIZIAMO IL CODE REFACTORING
        operations = new ArrayList<Operation>();
        
        // TODO NELLA CLASSE OPERATION INSERIRE IL METODO IN CUI CIASCUNO DI ESSI FA IL PROPRIO LAVORO
        
        file = new SimpleButton( 0, 0, widthB, heightB, FILE, Color.gray, index++, gc );
        float startX = file.getX() + width/400;
        save = new Operation( SAVE, startX, file.getMaxY(), widthB, heightB );
        load = new Operation( LOAD, startX, save.getMaxY(), widthB, heightB );
        operations.add( save );
        operations.add( load );
        FILES   = new MenuItem( file, operations );
        operations.clear();
        
        options = new SimpleButton( FILES.getMaxX(), 0, widthB, heightB, OPTIONS, Color.gray, index++, gc );
        OPTION  = new MenuItem( options, operations );
        operations.clear();

        edit    = new SimpleButton( options.getMaxX(), 0, widthB, heightB, EDIT, Color.gray, index++, gc );
        startX = edit.getX() + width/400;
        move   = new Operation( MOVE, startX, edit.getMaxY(), widthB, heightB );
        add    = new Operation( ADD, startX, move.getMaxY(), widthB, heightB );
        remove = new Operation( REMOVE, startX, add.getMaxY(), widthB, heightB );
        operations.add( move );
        operations.add( add );
        operations.add( remove );
        EDITING = new MenuItem( edit, operations );
        operations.clear();
        
        items = new ArrayList<MenuItem>();
        
        items.add( FILES );
        items.add( OPTION );
        items.add( EDITING );
        
        
        /*buttons = new ArrayList<SimpleButton>();
        
        file    = new SimpleButton( 0, 0, widthB, heightB, FILE, Color.gray, index++, gc );
        options = new SimpleButton( file.getMaxX(), 0, widthB, heightB, OPTIONS, Color.gray, index++, gc );
        edit    = new SimpleButton( options.getMaxX(), 0, widthB, heightB, EDIT, Color.gray, index++, gc );
        
        buttons.add( file );
        buttons.add( options );
        buttons.add( edit );
        
        saveload = new ArrayList<Operation>();
        
        saveload.add( save );
        saveload.add( load );
        
        operation = new ArrayList<Operation>();
        
        
        
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
        }*/
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
            
            for (MenuItem item: items) {
            	item.update( mouseX, mouseY, leftMouse, mouseDown );
            }
            
            /*for (SimpleButton button : buttons) {
                if (button.checkClick( mouseX, mouseY ) && !button.isPressed()) {
                    button.setPressed( true );
                }
            }*/
        } else if (!leftMouse && mouseDown) {
            mouseDown = false;
            
            for (MenuItem item: items) {
            	item.update( mouseX, mouseY, leftMouse, mouseDown );
            }
        }
        
        
        
        
        /*if (editing) {
            chooseType = areaType[0].contains( mouseX, mouseY );
        }
        
        if (opFile) {
        	for (Operation op: saveload) {
        		op.checkContains( mouseX, mouseY );
        	}
        } else {
	        if (chooseType) {
	        	for (Operation op: type) {
	        		op.checkContains( mouseX, mouseY );
	        	}
	        } 
	        
	        if (editing) {
	        	for (Operation op: operation) {
	        		op.checkContains( mouseX, mouseY );
	        	}
	        }
        }
        
        if (!nd.isAddingElement() && leftMouse && !mouseDown) {
            mouseDown = true;
            
            for (SimpleButton button : buttons) {
                if (button.checkClick( mouseX, mouseY ) && !button.isPressed()) {
                    button.setPressed( true );
                }
            }
        } else if (!leftMouse && mouseDown) {
            mouseDown = false;
            
            for (SimpleButton button: buttons) {
                if (button.isPressed()) {
                    for (SimpleButton bottone: buttons) {
                        if (bottone.isPressed()) {
                            bottone.setPressed( false );
                        }
                    }

                    if (button.checkClick( mouseX, mouseY )) {
                        if (button.getName().equals( FILE )) {
                            opFile = !opFile;
                            editing = false;
                        } else if (button.getName().equals( OPTIONS )) {
                            ;
                        } else if (button.getName().equals( EDIT )) {
                        	editing = !editing;
                        	opFile = false;
                        }
                    }
                }
            }
            
            if (opFile) {
            	for (Operation op: saveload) {
            		if (op.checkCollision( mouseX, mouseY )) {
            			if (op.getName().equals( SAVE )) {
            			} else if (op.getName().equals( LOAD )) {
            			}
            		}
            	}
            }

            if (!nd.isMoving() && !nd.isRemoving() && !nd.isAddingElement() && editing) {
                for (Operation op : operation) {
                	if (op.checkCollision( mouseX, mouseY )) {
		            	if (op.getName().equals( MOVE ) && !nd.isRemoving()) {
	                        nd.setNodeSelectable();
	                    } else if (op.getName().equals( REMOVE ) && !nd.isMoving()) {
	                        nd.removeNode();
	                    } else {
	                        break;
	                    }
    	            	
                        chooseType = false;
    	            	editing = false;
                	}
                }
            }
            
            if (!nd.isMoving() && !nd.isRemoving() && chooseType) {
                if (!nd.isInExecution()) {
                    for (Operation op: type) {
                        if (op.checkCollision( mouseX, mouseY )) {
                            if (op.getName().equals( CLIENT )) {
                                nd.addNode( mouseX, mouseY, "client" );
                                nd.setAddingNode();
                            } else if (op.getName().equals( SERVER )) {
                                nd.addNode( mouseX, mouseY, "server" );
                                nd.setAddingNode();
                            } else if (op.getName().equals( SWITCH )) {
                                nd.addNode( mouseX, mouseY, "switch" );
                                nd.setAddingNode();
                            } else if (op.getName().equals( PACKET )) {
                                nd.addPacket( mouseX, mouseY );
                                nd.setAddingPacket();
                            }
                            
                            editing = false;
                            chooseType = false;
                            
                            break;
                        }
                    }
                }
            }
        }*/
    }
    
    @Override
    public void render( final GameContainer gc ) {
        Graphics g = gc.getGraphics();
        
        for (MenuItem item: items) {
        	item.render( g );
        }
        
        /*for (SimpleButton button: buttons) {
            button.draw( g );
        }
        
        if (editing) {
        	for (Operation op: operation) {
        		op.render( g );
        	}
        	
        	if (chooseType) {
        	    for(Operation op: type) {
        	        op.render( g );
        	    }
        	}
        }

    	if (opFile) {
    		for (Operation op: saveload) {
    			op.render( g );
        	}
    	}*/
	}
    
    public void resetAllButtons() {
        for (SimpleButton button: buttons) {
            if (button.isPressed()) {
                button.setPressed( false );
            }
        }
    }
}
