package simulator.graphics.dataButton;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Shape;

import simulator.graphics.elements.Menu;
import simulator.graphics.elements.Operation;

public class MenuItem extends Button
{
	private SimpleButton button;
	private List<Operation> operations;
	private Shape[] areaType = null;
	private int index = -1;
	
	private List<Menu> menu;
	
	public MenuItem( final SimpleButton button, final ArrayList<Operation> operations ) throws SlickException
	{
		this.button = button;
		this.operations = new ArrayList<Operation>( operations );
		
		menu = new ArrayList<Menu>();
		
		// TODO APPENA OPTIONS AVRA OPERAZIONI QUESTO IF VERRA ELIMINATO
		if (this.operations.size() > 0) {
    		areaType = new Shape[] {this.operations.get( 0 ).getArea()};
    		
    		for (int i = 0; i < operations.size(); i++) {
                menu.add( new Menu() );
            }
		}
	}
	
	public void addItem( final Operation op, final ArrayList<Operation> ops ) {
        areaType = areaType[0].union( op.getArea() );
	    for (Operation ope: ops) {
	        areaType = areaType[0].union( ope.getArea() );
	    }
	    
	    for (int i = 0; i < operations.size(); i++) {
            if (operations.get( i ).equals( op )) {
                menu.remove( i );
                menu.add( i, new Menu( op, ops, index ) );
            }
        }
	    
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
		
		if (button.isPressed()) {
            for (int i = 0; i < operations.size(); i++) {
                if (operations.get( i ).checkContains( mouseX, mouseY )) {
                    index = i;
                }
            }
            
		    if (index != -1) {
		        if (operations.get( index ).checkContains( mouseX, mouseY )
		         || areaType[0].contains( mouseX, mouseY )) {
		            menu.get( index ).update( mouseX, mouseY );
		        } else {
		            index = -1;
		        }
		    }
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