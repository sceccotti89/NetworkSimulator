package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Packet implements Comparable<Packet>
{
	private long ID_from, ID_to;
	
	private Color color;
	
	private Rectangle area;
	
	private float width;
	
	private boolean hasFinished, isInNode;
	
	private long nextNode;
	
	private long indexRotation;
	
	private int startTime, endTime;
	
	private String name;
	
	private boolean active;
	
	private float speedX, speedY;
	
	private float angle;
	
	private float widthInfo, heightInfo;
	private Rectangle infos;
	private boolean drawInfo;
	
	public Packet( final GameContainer gc, final int x, final int y, final long ID_from, final long ID_to, final Color color, final int startTime, final int endTime ) {
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		
		width = gc.getWidth()/80;
		
		area = new Rectangle( x, y + gc.getHeight()/150, width, width );
		
		hasFinished = false;
		isInNode = true;
		
		nextNode = ID_from;
		indexRotation = ID_from;
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		active = true;
		
		widthInfo  = gc.getWidth()/7;
		heightInfo = gc.getHeight()/13;
		
		infos = new Rectangle( 0, 0, widthInfo, heightInfo );
		
		drawInfo = false;
	}
	
	public float getSpeedX() {
		return speedX;
	}
	
	public float getSpeedY() {
		return speedY;
	}
	
	public void setSpeed( final float lenght, final float angle ) {
		this.angle = angle;
		
		float rad = (float) (angle * Math.PI / 180);
		
		// TODO SETTARE LA VELOCITA IN RELAZIONE ALLA LUNGHEZZA DEL LINK
		speedX = (float) Math.sin( rad );
		speedY = (float) Math.cos( rad );
	}
	
	public boolean isActive() {
		return active;
	}
	
	public void setActive( boolean val ) {
		active = val;
	}
	
	public void setStartTime( int val ) {
		startTime = val;
	}
	
	public int getStartTime() {
		return startTime;
	}
	
	public void setEndTime( int val ) {
		endTime = val;
	}
	
	public int getEndTime() {
		return endTime;
	}
	
	public void setIndexRotation( int val ) {
		indexRotation = val;
	}
	
	public long getIndexRotation() {
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
	
	public long getNextNode() {
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
		return area;
	}
	
	public void setColor( Color color ){
		this.color = color;
	}
	
	public long getIDFrom(){
		return ID_from;
	}
	
	public long getDestID(){
		return ID_to;
	}
	
	public void update( final int animTime, GameContainer gc, boolean animate ) {		
		float mouseX = gc.getInput().getMouseX();
		float mouseY = gc.getInput().getMouseY();
		
		if (animate && active) {
			area.setLocation( area.getX() + speedX * animTime, area.getY() + speedY * animTime );
		}
		
		if (area.contains( mouseX, mouseY )) {
			drawInfo = true;
			
			float offset = gc.getWidth()/80;
			infos.setLocation( mouseX + offset, mouseY - offset );
		} else {
			drawInfo = false;
		}
	}
	
	public void draw( final Graphics g ) {
		g.rotate( area.getX() + area.getWidth()/2, area.getY() + area.getHeight()/2, angle );		
		g.setColor( color );
		g.fill( area );		
		g.resetTransform();
		
		if (drawInfo) {
			Font f = g.getFont();
			String info = "ID_From = " + ID_from + "\n" + "ID_To = " + ID_to;
			infos.setSize( f.getWidth( info ), f.getHeight( info ) );
			
			g.setColor( Color.magenta );
			g.fill( infos );
			g.setColor( Color.black );
			g.draw( infos );
			
			g.drawString( info, infos.getX(), infos.getY() );
		}
	}

	@Override
	public int compareTo( Packet packet ) {
		if (startTime < packet.startTime) return -1;
		if (startTime > packet.startTime) return 1;
		return 0;
	}
}
