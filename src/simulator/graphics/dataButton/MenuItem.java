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
	private boolean viewAreaType = false;
	private List<Operation> area;
	private int index = -1;
	
	// TODO UTILIZZARE LA CALSSE MENU
	private List<Menu> menu;
	
	// TODO NEL CASO DI MENU A TENDINA CONCATENATE, DOVRO PENSARE DI FARE UNA CLASSE APPOSTA PER GESTIRLE E AREATYPE
	
	public MenuItem( final SimpleButton button, final ArrayList<Operation> operations ) throws SlickException
	{
		this.button = button;
		this.operations = new ArrayList<Operation>( operations );
	}
	
	public void addItem( final Operation op, final ArrayList<Operation> ops ) {
		// TODO UTILIZZARE LA CLASSE MENU
		for (Menu m: menu) {
			if (m.checkButton( op )) {
				// TODO PENSARE A COME GESTIRE LA COSA
				m.addItems( op, ops );
				return;
			}
		} 
		
		// TODO INSERIRE UN NUOVO MENU
		for (int i = 0; i < operations.size(); i++) {
			if (operations.get( i ).equals( op )) {
				index = i;
				menu.add( new Menu( op, ops, index ) );
			}
		}
		
		/*area = new ArrayList<Operation>(ops);
		for (int i = 0; i < operations.size(); i++) {
			if (operations.get( i ).equals( op )) {
				index = i;
			}
		}
		areaType = new Shape[] {op.getArea()};
        
        for (Operation ope: ops) {
            areaType = areaType[0].union( ope.getArea() );
        }*/
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
		
		// TODO UTILIZZARE I MENU
		for (Menu m: menu) {
			m.update( mouseX, mouseY );
		}
		
		
		
		
		/*if (!viewAreaType && index != -1) {
			viewAreaType = operations.get( index ).checkCollision( mouseX, mouseY );
		}
		
		if (viewAreaType) {
			if (!areaType[0].contains( mouseX, mouseY )) {
				viewAreaType = false;
			} else {
				for (Operation op: area) {
					op.checkContains( mouseX, mouseY );
					if (op.checkCollision( mouseX, mouseY )) {
						// TODO FA IL SUO LAVORO
					}
				}
			}
		}
		
		for (Operation op: operations) {
			op.checkContains( mouseX, mouseY );
			if (op.checkCollision( mouseX, mouseY )) {
				// TODO FAR ESEGUIRE LA PROPRIA OPERAZIONE
			}
		}
		
		if (viewAreaType) {
			operations.get( index ).setSelected( true );
		}*/
	}
	
	public void render( Graphics g ) {
		button.draw( g );
		
		if (button.isPressed()) {
			for (Menu m: menu) {
				m.render( g );
			}
			
			
			/*for (Operation op: operations) {
				op.render( g );
			}*/
		}
	}
}