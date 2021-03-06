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
	
	public Menu ( Operation start, ArrayList<Operation> ops ) {
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
    
    public void addItems( Operation op, ArrayList<Operation> ops ) {
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
    
    private void executeOperation( int mouseX, int mouseY, NetworkDisplay nd ) throws SlickException {
    	for (Operation op: ops) {
    		if (op.checkCollision( mouseX, mouseY )) {
    			op.execute( mouseX, mouseY, nd );
    			return;
    		}
    	}	
    }
	
	public boolean checkContains( int mouseX, int mouseY, boolean leftMouse, NetworkDisplay nd, Event event ) throws SlickException {
	    if (leftMouse) {
	        if (index != -1) {
	            if (areaType.contains( mouseX, mouseY )) {
	            	executeOperation( mouseX, mouseY, nd );
	                return false;
	            } else {
	                if (menu.size() >= index + 1) {
	                    if (!menu.get( index ).checkContains( mouseX, mouseY, leftMouse, nd, event )) {
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
	
	public boolean checkClick( int mouseX, int mouseY, Event event, NetworkDisplay nd ) throws SlickException {
		for (Operation op: ops) {
			if (op.checkContains( mouseX, mouseY )) {
				executeOperation( mouseX, mouseY, nd );
				event.setConsumed( true );
				return true;
			}
		}
		
		boolean click = false;
		for (Menu m: menu) {
			click = click || m.checkClick( mouseX, mouseY, event, nd );
		}
		
		if (click) {
			event.setConsumed( true );
			
			for (Menu m: menu) {
				m.resetIndex();
			}
		}
		
		return click;
	}
	
	public void update( int mouseX, int mouseY, boolean leftMouse, NetworkDisplay nd, Event event ) throws SlickException {
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
                m.checkContains( mouseX, mouseY, leftMouse, nd, event );
            }
        }
        
        if (index != -1) {
            if (menu.size() >= index + 1) {
                menu.get( index ).update( mouseX, mouseY, leftMouse, nd, event );
            }
        }
	}
	
	public void render( Graphics g ) {
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
