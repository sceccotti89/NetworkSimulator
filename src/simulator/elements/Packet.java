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
	
	// TODO CHIARIRE A COSA SERVA
	private String name;
	
	public Packet( final GameContainer gc, final float x, final float y, final int ID_from, int ID_to, Color color ) {
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		
		width = gc.getWidth()/80;
		
		pack = new Rectangle( x, y - width/2, width, width );
		
		hasFinished = false;
		isInNode = false;
		
		// TODO SETTARE QUESTO PARAMETRO (QUANDO STEFANO CHIARIRA QUESTO PUNTO) DA INPUT
		nextNode = 0;
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
	
	public void draw( final Graphics g ) {
		g.setColor( color );
		g.fill( pack );
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
}
