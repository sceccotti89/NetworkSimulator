package simulator.graphics.elements;

import java.util.ArrayList;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Shape;

public class Menu
{
	private Operation start;
	private ArrayList<Operation> ops;
	private ArrayList<Menu> menu;
	// TODO PENSARE A COME GESTIRLO
	private Shape[] areaType = null;
	private int index;
	
	public Menu ( final Operation start, final ArrayList<Operation> ops, final int index ) {
		this.start = start;
		this.ops = new ArrayList<Operation>(ops);
	}
	
	/**empty builder*/
	public Menu () {
	    ops = new ArrayList<Operation>();
        menu = new ArrayList<Menu>();
	}
	
	public int getSizeOp() {
	    return ops.size();
	}
	
	public void addItems( final Operation op, final ArrayList<Operation> ops ) {
		// TODO DA DEFINIRE
		for (int i = 0; i < ops.size(); i++) {
			if (ops.get( i ).equals( op )) {
				index = i;
				menu.add( new Menu( op, ops, index ) );
			}
		}
	}
	
	public boolean checkButton( Operation op ) {
		for (Operation ope: ops) {
			if (ope.equals( start )) {
				return true;
			}
		}
		
		return false;
	}
	
	public void update( final int mouseX, final int mouseY ) {
		for (Operation op: ops) {
			op.checkContains( mouseX, mouseY );
		}
		
		// TODO CONTROLLARE IL RESTO DEI BOTTONI E UN EVENTUALE TENDINA A CATENA
		
	}
	
	public void render( final Graphics g ) {
		for (Operation op: ops) {
		    op.render( g );
		}
	}
}