package simulator.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Packet
{
	private int ID_from, ID_to;
	
	private Color color;
	
	private Rectangle pack;
	
	private float width;
	
	private boolean hasFinished, isInNode;
	
	private int nextNode;
	
	private int indexRotation;
	
	private int time;
	
	private String name;
	
	private boolean active;
	
	public Packet( final GameContainer gc, final float x, final float y, final int ID_from, int ID_to, Color color, int time ) {
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		
		width = gc.getWidth()/80;
		
		pack = new Rectangle( x, y - width/2, width, width );
		
		hasFinished = false;
		isInNode = true;
		
		nextNode = ID_from;
		indexRotation = ID_from;
		
		this.time = time;
		
		active = true;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive( boolean val ) {
		active = val;
	}
	
	public void setTime( int val ) {
		time = val;
	}
	
	public int getTime() {
		return time;
	}
	
	public void setIndexRotation( int val ) {
		indexRotation = val;
	}
	
	public int getIndexRotation() {
		return indexRotation;
	}
	
	public void setFinished( boolean val ) {
		hasFinished = val;
	}
	
	public boolean getFinished() {
		return hasFinished;
	}
	
	public void setIsInNode( boolean val ) {
		isInNode = val;
	}
	
	public boolean getIsInNode() {
		return isInNode;
	}
	
	public void incNextNode() {
		nextNode++;
	}
	
	public int getNextNode() {
		return nextNode;
	}
	
	public void setNextNode( int val ) {
		nextNode = val;
	}
	
	public void setIDFrom( final int IDFrom ) {
		ID_from = IDFrom;
	}
	
	public void setIDTo( final int IDTo ) {
		ID_to = IDTo;
	}
	
	public Rectangle getArea(){
		return pack;
	}
	
	public void setColor( Color color ){
		this.color = color;
	}
	
	public int getIDFrom(){
		return ID_from;
	}
	
	public int getIDTo(){
		return ID_to;
	}
	
	public void draw( final Graphics g ) {
		g.setColor( color );
		g.fill( pack );
	}
}
