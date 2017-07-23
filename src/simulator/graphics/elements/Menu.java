package simulator.graphics.elements;

import java.util.ArrayList;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.interfaces.NetworkDisplay;

public class Menu
{
	private ArrayList<Operation> ops;
	private ArrayList<Menu> menu;
	private Rectangle areaType = null;
	private int index = -1;
	
	public Menu ( final Operation start, final ArrayList<Operation> ops ) {
		this.ops = new ArrayList<Operation>(ops);
        
        areaType = new Rectangle( ops.get( 0 ).getX(), ops.get( 0 ).getY(),
                                  ops.get( 0 ).getWidth(),
                                  ops.get( ops.size() - 1 ).getMaxY() - ops.get( 0 ).getY());

        menu = new ArrayList<Menu>();
	}
	
	public Menu () {
	    ops = new ArrayList<Operation>();
        menu = new ArrayList<Menu>();
	}
    
    public void addItems( final Operation op, final ArrayList<Operation> ops ) {
        for (int i = 0; i < this.ops.size(); i++) {
            menu.add( new Menu() );
        }
        
        for (int i = 0; i < this.ops.size(); i++) {
            if (this.ops.get( i ).equals( op )) {
                menu.remove( i );
                menu.add( i, new Menu( op, ops ) );
                return;
            }
        }
    }
    
    public void resetIndex() {
        index = -1;
        for (Menu m: menu) {
            m.resetIndex();
        }
    }
    
    private void executeOperation( final int mouseX, final int mouseY, final NetworkDisplay nd ) throws SlickException {
    	for (Operation op: ops) {
    		if (op.checkCollision( mouseX, mouseY )) {
    			op.execute( mouseX, mouseY, nd );
    			return;
    		}
    	}	
    }
	
	public boolean checkContains( final int mouseX, final int mouseY, final boolean leftMouse, final NetworkDisplay nd ) throws SlickException {
	    if (leftMouse) {
	        if (index != -1) {
	            if (areaType.contains( mouseX, mouseY )) {
	            	executeOperation( mouseX, mouseY, nd );
	                return false;
	            } else {
	                if (menu.size() >= index + 1) {
	                    if (!menu.get( index ).checkContains( mouseX, mouseY, leftMouse, nd )) {
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
	
	public boolean checkButton( Operation op ) {
	    for (Operation ope: ops) {
	        if (ope.equals( op )) {
	            return true;
	        }
	    }
	    
	    for (int i = 0; i < menu.size(); i++) {
	        if (menu.get( i ).checkButton( op )) {
	            return true;
	        }
	    }
	    
		return false;
	}
	
	public void update( final int mouseX, final int mouseY, final boolean leftMouse, final NetworkDisplay nd ) throws SlickException {
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
                m.checkContains( mouseX, mouseY, leftMouse, nd );
            }
        }
        
        if (index != -1) {
            if (menu.size() >= index + 1) {
                menu.get( index ).update( mouseX, mouseY, leftMouse, nd );
            }
        }
	}
	
	public void render( final Graphics g ) {
	    if (index != -1) {
	        if (menu.size() >= index + 1) {
	            menu.get( index ).render( g );
	        }
	        
	        ops.get( index ).setSelected( true );
	    }
	    
		for (Operation op: ops) {
		    op.render( g );
		}
	}
}