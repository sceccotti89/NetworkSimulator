package simulator.graphics.dataButton;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.elements.Menu;
import simulator.graphics.elements.Operation;

public class MenuItem extends Button
{
	private SimpleButton button;
	private List<Operation> operations;
	private Rectangle areaType = null;
	private int index = -1;
	
	// TODO PENSARE DI USARE VETTORE DI MENUITEM INVECE DI MENU (ELIMINEREI QUEST'ULTIMA CLASSE)
	private List<Menu> menu;
	
	public MenuItem( final SimpleButton button, final ArrayList<Operation> operations ) throws SlickException
	{
		this.button = button;
		this.operations = new ArrayList<Operation>( operations );
		
		menu = new ArrayList<Menu>();
		
		areaType = new Rectangle( operations.get( 0 ).getX(), operations.get( 0 ).getY(),
		                          operations.get( 0 ).getWidth(),
		                          operations.get( operations.size() - 1 ).getMaxY() - operations.get( 0 ).getY());
    		
		for (int i = 0; i < operations.size(); i++) {
            menu.add( new Menu() );
        }
	}
	
	public void addItem( final Operation op, final ArrayList<Operation> ops ) {
	    // l'operazione era tra quelli della tendina
	    for (int i = 0; i < operations.size(); i++) {
	        if (operations.get( i ).equals( op )) {
	            menu.remove( i );
	            menu.add( i, new Menu( op, ops ) );
	            return;
	        }
	    }
	    
	    // itero sui vari sotto-menu a tendina
	    for (Menu m: menu) {
	        if (m.checkButton( op )) {
	            m.addItems( op, ops );
	            return;
	        }
	    }
	}
	
	public float getX() {
		return button.getX();
	}
	
	public float getMaxX() {
		return button.getMaxX();
	}
	
	public float getMaxY() {
		return button.getMaxY();
	}
	
	public void update( final int mouseX, final int mouseY, final boolean leftMouse, final boolean mouseDown ) {
		if (leftMouse && mouseDown) {
			if (button.checkClick( mouseX, mouseY ) && !button.isPressed()) {
	            button.setPressed( true );
	        }
		} else if (!leftMouse && !mouseDown) {
			if (button.isPressed()) {
				if (button.checkClick( mouseX, mouseY )) {
					button.setPressed( false );
				}
			}
		}
		
		// FINO A QUI...TUTTO..BENE!
		boolean find = false;
		for (int i = 0; i < operations.size(); i++) {
            Operation op = operations.get( i );
            if (op.checkContains( mouseX, mouseY )) {
                if (index != i && index != -1) {
                    for (Menu m: menu) {
                        m.resetIndex();
                    }
                }
                index = i;
                find = true;
            }
        }
		
		if (!find) {
    		for (Menu m: menu) {
    		    find = find || m.checkContains( mouseX, mouseY, leftMouse );
    		}
		}
		
		if (!find) {
		    // TODO CHIUDERE TUTTE LE TENDINE E SOTTOTENDINE
            for (Menu m: menu) {
                m.resetIndex();
            }
            
            index = -1;
            //button.setPressed( false );
		}
		
		if (index != -1) {
		    menu.get( index ).update( mouseX, mouseY, leftMouse );
		}
	}
	
	public void render( Graphics g ) {
		button.draw( g );
		
		if (button.isPressed()) {
            if (index != -1) {
                menu.get( index ).render( g );
                
                operations.get( index ).setSelected( true );
            }
            
			for (Operation op: operations) {
				op.render( g );
			}
		}
	}
}