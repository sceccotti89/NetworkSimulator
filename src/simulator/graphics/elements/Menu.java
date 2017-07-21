package simulator.graphics.elements;

import java.util.ArrayList;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Menu
{
	private Operation start;
	private ArrayList<Operation> ops;
	private ArrayList<Menu> menu;
	// TODO PENSARE A COME GESTIRLO
	private Rectangle areaType = null;
	private int index = -1;
	private int pos = -1;
	
	public Menu ( final Operation start, final ArrayList<Operation> ops ) {
		this.start = start;
		this.ops = new ArrayList<Operation>(ops);
        
        areaType = new Rectangle( ops.get( 0 ).getX(), ops.get( 0 ).getY(),
                                  ops.get( 0 ).getWidth(),
                                  ops.get( ops.size() - 1 ).getMaxY() - ops.get( 0 ).getY());

        menu = new ArrayList<Menu>();
	}
	
	/**empty builder*/
	public Menu () {
	    ops = new ArrayList<Operation>();
        menu = new ArrayList<Menu>();
	}
    
    public void addItems( final Operation op, final ArrayList<Operation> ops ) {
        for (int i = 0; i < this.ops.size(); i++) {
            menu.add( new Menu() );
        }
        
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get( i ).equals( op )) {
                System.out.println( "INDEX = " + index );
                menu.remove( i );
                menu.add( i, new Menu( op, ops ) );
                return;
            }
        }
        
        if (pos != -1) {
            menu.get( pos ).addItems( op, ops );
        }
    }
	
	public boolean checkContains( final float mouseX, final float mouseY, final boolean leftMouse ) {
	    if (leftMouse) {
	        if (index != -1) {
	            if (areaType.contains( mouseX, mouseY )) {
	                return true;
	            } else {
	                if (menu.get( index ) != null) {
	                    if (!menu.get( index ).checkContains( mouseX, mouseY, leftMouse )) {
	                        index = -1;
	                        return false;
	                    } else {
	                        return true;
	                    }
	                } else {
	                    index = -1;
	                    return false;
	                }
	            }
	        } else {
	            return false;
	        }
	    }
	    
	    return true;
	}
	
	public int getSizeOp() {
	    return ops.size();
	}
	
	public boolean checkButton( Operation op ) {
	    for (Operation ope: ops) {
	        if (ope.equals( op )) {
	            return true;
	        }
	    }
	    
	    for (int i = 0; i < menu.size(); i++) {
	        if (menu.get( i ).checkButton( op )) {
	            pos = i;
	            return true;
	        }
	    }
	    
		return false;
	}
	
	public void update( final int mouseX, final int mouseY, final boolean leftMouse ) {
	    boolean find = false;
        for (int i = 0; i < ops.size(); i++) {
            Operation op = ops.get( i );
            if (op.checkContains( mouseX, mouseY )) {
                index = i;
                find = true;
            }
        }
        
        if (!find) {
            for (Menu m: menu) {
                m.checkContains( mouseX, mouseY, leftMouse );
            }
        }
        
        if (index != -1) {
            System.out.println( "INDEX = " + index );
            menu.get( index ).update( mouseX, mouseY, leftMouse );
        }
	}
	
	public void render( final Graphics g ) {
	    if (index != -1) {
	        ops.get( index ).setSelected( true );
	    }
	    
		for (Operation op: ops) {
		    op.render( g );
		}
	}
}