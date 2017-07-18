package simulator.graphics.dataButton;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Shape;

import simulator.graphics.elements.Operation;

public class MenuItem extends Button
{
	private SimpleButton button;
	private List<Operation> operations;
	private Shape[] areaType;
	
	public MenuItem( final SimpleButton button, final ArrayList<Operation> operations ) throws SlickException
	{
		this.button = button;
		this.operations = new ArrayList<Operation>( operations );
	}
	
	public void addItem( final Operation op, final ArrayList<Operation> ops ) {
		operations.add( op );
        areaType = areaType[0].union( op.getArea() );
        
        for (Operation ope: ops) {
            areaType = areaType[0].union( ope.getArea() );
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
		} else if (!leftMouse && mouseDown) {
			if (!button.checkClick( mouseX, mouseY )) {
				button.setPressed( false );
			}
		}
		
		for (Operation op: operations) {
			op.checkContains( mouseX, mouseY );
			if (op.checkCollision( mouseX, mouseY )) {
				// TODO FAR ESEGUIRE LA PROPRIA OPERAZIONE
			}
		}
	}
	
	public void render( Graphics g ) {
		button.draw( g );
		
		// TODO SETTARE UN PARAMETRO PER DETERMINARE SE DISEGNARLO O NO
		if (button.isPressed()) {
			for (Operation op: operations) {
				op.render( g );
			}
		}
	}
}