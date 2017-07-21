
package simulator.graphics.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.MenuItem;
import simulator.graphics.dataButton.SimpleButton;
import simulator.graphics.elements.Operation;

public class OptionBar implements AnimationInterface
{
    private Rectangle barOptions = new Rectangle( 0, 0, 800, 20 );
    
    
    
    // TODO REALIZZARE I BOTTONI A TENDINA TRAMITE CLASSE
    private /*final*/ MenuItem FILES, OPTION, EDITING;
    private final List<MenuItem> items;
    
    
    private final SimpleButton file, options, edit;
    private final Operation save, load;
    private final Operation option;
    private final Operation move, add, remove;
    private Operation client, server, switcher, packet, test;
    
    private float widthB, heightB;
    
    private List<SimpleButton> buttons;

    private int mouseX, mouseY;
    
    private String FILE = "File", OPTIONS = "Options", EDIT = "Edit",
    			   SAVE = "Save File", LOAD = "Load File", OPZIONI = "Option",
                   MOVE = "MoveNode", ADD = "AddElement", REMOVE = "RemoveNode",
                   CLIENT = "Client", SERVER = "Server", SWITCH = "Switch", PACKET = "Packet";
    
    private boolean mouseDown;
    
	private ArrayList<Operation> operations;
    
    public OptionBar( final GameContainer gc, final int width, final int height ) throws SlickException
    {
        widthB = width/10;
        heightB = height/30;
        
        int index = 0;
        
        
        // TODO INIZIAMO IL CODE REFACTORING
        operations = new ArrayList<Operation>();
        
        // TODO NELLA CLASSE OPERATION INSERIRE IL METODO IN CUI CIASCUNO DI ESSI FA IL PROPRIO LAVORO
        // ANCHE SE RAGIONANDOCI NON PENSO SI POSSA FARE, VEDREMO
        
        file = new SimpleButton( 0, 0, widthB, heightB, FILE, Color.gray, index++, gc );
        float startX = file.getX() + width/400;
        save = new Operation( SAVE, startX, file.getMaxY(), widthB, heightB );
        load = new Operation( LOAD, startX, save.getMaxY(), widthB, heightB );
        operations.add( save );
        operations.add( load );
        FILES = new MenuItem( file, operations );
        operations.clear();
        
        options = new SimpleButton( FILES.getMaxX(), 0, widthB, heightB, OPTIONS, Color.gray, index++, gc );
        startX = options.getX() + width/400;
        option = new Operation( OPZIONI, startX, options.getMaxY(), widthB, heightB );
        operations.add( option );
        OPTION = new MenuItem( options, operations );
        operations.clear();

        edit   = new SimpleButton( options.getMaxX(), 0, widthB, heightB, EDIT, Color.gray, index++, gc );
        startX = edit.getX() + width/400;
        move   = new Operation( MOVE, startX, edit.getMaxY(), widthB, heightB );
        add    = new Operation( ADD, startX, move.getMaxY(), widthB, heightB );
        remove = new Operation( REMOVE, startX, add.getMaxY(), widthB, heightB );
        operations.add( move );
        operations.add( add );
        operations.add( remove );
        EDITING = new MenuItem( edit, operations );
        
        operations.clear();
        startX = move.getMaxX();
        client   = new Operation( CLIENT, startX, move.getY(), widthB, heightB );
        server   = new Operation( SERVER, startX, client.getMaxY(), widthB, heightB );
        switcher = new Operation( SWITCH, startX, server.getMaxY(), widthB, heightB );
        packet   = new Operation( PACKET, startX, switcher.getMaxY(), widthB, heightB );
        operations.add( client );
        operations.add( server );
        operations.add( switcher );
        operations.add( packet );
        EDITING.addItem( move, operations );
        
        operations.clear();
        startX = server.getMaxX();
        test   = new Operation( CLIENT, startX, client.getY(), widthB, heightB );
        operations.add( test );
        EDITING.addItem( server, operations );
        
        operations.clear();
        
        items = new ArrayList<MenuItem>();
        
        items.add( FILES );
        items.add( OPTION );
        items.add( EDITING );
    }
    
    public float getMaxY() {
        return barOptions.getMaxY();
    }
    
    @Override
    public void update( final int delta, final GameContainer gc, final boolean leftMouse, final NetworkDisplay nd ) throws SlickException
    {
        mouseX = gc.getInput().getMouseX();
        mouseY = gc.getInput().getMouseY();
        
        // TODO RAGIONARE UN PO SUI PULSANTI PREMUTI
        if (leftMouse && !mouseDown) {
            mouseDown = true;
        } 
        
        // TODO PER ORA LO LASCIO IN COMMENTO PER LAVORARCI, POI SI VEDRA
        /*else if (!leftMouse && mouseDown) {
            mouseDown = false;
        }*/
        
        for (MenuItem item: items) {
        	item.update( mouseX, mouseY, leftMouse, mouseDown );
        }
    }
    
    @Override
    public void render( final GameContainer gc ) {
        Graphics g = gc.getGraphics();
        
        for (MenuItem item: items) {
        	item.render( g );
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
