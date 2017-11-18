package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;

import simulator.graphics.interfaces.NetworkDisplay;

public class Operation
{
	private final Rectangle area, zone;
	
	private boolean selected = false;
	
	private String name;
	
	private final String SAVE = "Save File", LOAD = "Load File",
			       		 MOVE = "MoveNode", REMOVE = "RemoveNode",
			       		 CLIENT = "Client", SERVER = "Server", SWITCH = "Switch", PACKET = "Packet";
	
	public Operation( String name,  float x, float y, float width, float height )
	{
		this.name = name;
		
		area = new Rectangle( x, y, width + width/4, height );
		zone = new Rectangle( x, y, width + width/4, height );
	}
	
	public boolean checkCollision( int mouseX, int mouseY ) {
		return area.contains( mouseX, mouseY );
	}
	
	public boolean intersects( float mouseX, float mouseY ) {
	    return area.intersects( new Rectangle( mouseX, mouseY, 1, 1 ) );
	}
	
	public float getX() {
		return area.getX();
	}
	
	public float getY(){
	    return area.getY();
	}
	
	public float getWidth() {
	    return area.getWidth();
	}
	
	public float getMaxX(){
	    return area.getMaxX();
	}
	
	public float getMaxY() {
		return area.getMaxY();
	}
	
	public String getName() {
		return name;
	}
	
	public Shape getArea() {
	    return area;
	}
	
	public boolean checkContains( float mouseX, float mouseY ) {
		return selected = area.contains( mouseX, mouseY );
	}
	
	public void setSelected( boolean val ) {
		selected = val;
	}
	
	public void execute( int mouseX, int mouseY, NetworkDisplay nd ) throws SlickException {
		if (!nd.isInExecution()) {
			switch ( name ) {
			
			case CLIENT:
				if (!nd.isMoving() && !nd.isRemoving()) {
					nd.addNode( mouseX, mouseY, CLIENT );
				}
				break;
				
			case SERVER:
				if (!nd.isMoving() && !nd.isRemoving()) {
					nd.addNode( mouseX, mouseY, SERVER );
				}
				break;
				
			case SWITCH:
				if (!nd.isMoving() && !nd.isRemoving()) {
					nd.addNode( mouseX, mouseY, SWITCH );
				}
				break;
				
			case PACKET:
				if (!nd.isMoving() && !nd.isRemoving()) {
					nd.addPacket(mouseX, mouseY);
				}
				break;
				
			case REMOVE:
				if (!nd.isMoving() && !nd.isAddingElement()) {
					nd.removeNode();
				}
				break;
				
			case MOVE:
				if (!nd.isRemoving() && !nd.isAddingElement()) {
					nd.moveNode();
				}
				break;
				
			case SAVE:
				// TODO DA IMPLEMENTARE
				break;
				
			case LOAD:
				// TODO DA IMPLEMENTARE
				break;
	
			default:
				break;
			}
		}
	}
	
	public void render( Graphics g ) {
		g.setColor( Color.white );
		g.fill( area );
		g.setColor( Color.black );
		g.draw( area );
		
		if (selected) {
			Color color = new Color( Color.blue );
			color.a = 0.5f;
			g.setColor( color );
			g.fill( zone );
		}
		
		g.setColor( Color.black );
		
		int fWidth = g.getFont().getWidth( name ), fHeight = g.getFont().getHeight( name );
		g.drawString( name, area.getCenterX() - fWidth/2, area.getCenterY() - fHeight/2 );
	}
}
