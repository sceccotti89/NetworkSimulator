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
	
	private boolean hasFinished, isInNode;
	
	private long nextNode;
	
	private long indexRotation;
	
	private long startTime, endTime;
	
	private boolean active;
	
	private float speedX, speedY;
	
	private float angle;
	
	private float widthInfo, heightInfo;
	private Rectangle infos;
	private boolean drawInfo;
	
	private int width, height;
	
	private float linkLenght;
	
	private float distance;
	
	private float startX, startY;
	
	private int mouseX, mouseY;
	
	private int timer;
	
	public Packet( final int x, final int y,
				   final long ID_from, final long ID_to,
				   final Color color,
				   final long startTime, final long endTime,
				   final int width, final int height ) {
		
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
		this.height = height;
		this.width = width;
		
		area = new Rectangle( x + width/32, y, width/80, height/60 );
		
		hasFinished = false;
		isInNode = true;
		
		nextNode = ID_from;
		indexRotation = ID_from;
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		active = true;
		
		widthInfo  = width/7;
		heightInfo = height/13;
		
		infos = new Rectangle( 0, 0, widthInfo, heightInfo );
		
		drawInfo = false;
		
		speedY = 0;
		
		distance = 0;
		
		startX = x;
		startY = y;
		
		timer = 0;
	}
	
	public void initializingSpeed( final Node from, final Node dest ) {
		if (from.getCenterX() < dest.getCenterX()) {
			speedX = 1;
		} else if (from.getCenterX() > dest.getCenterX()) {
			speedX = -1;
		} else speedX = 0;
	}
	
	public float getSpeedX() {
		return speedX;
	}
	
	public float getSpeedY() {
		return speedY;
	}
	
	public void setLinkLenght( float val ) {
		linkLenght = val;
	}
	
	public float getLinkLenght() {
		return linkLenght;
	}
	
	/**metodo per riattivare i pacchetti nel caso di click nella barra*/
	public void setConditions( final Node node, final long time, final int frames ) {
		long point = time - startTime;
		if (point <= 0) {
			distance = 0;
		} else {
			// TODO DA QUI IN POI NON TORNA NULLA...C'E DA RAGIONARCI UN PO'
			distance = speedX * point;
			System.out.println( "DISTANCE = " + distance );
		}
		
		area.setLocation( node.getCenterX() + width/32 + distance, node.getCenterY() + height/30 );
		active = true;
	}
	
	public void setAngle( float val ) {
		angle = val;
	}
	
	public void setSpeed( final int frames ) {
		speedX = ((linkLenght - distance) / (endTime - startTime - timer)) * frames;
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
	
	public long getStartTime() {
		return startTime;
	}
	
	public void setEndTime( int val ) {
		endTime = val;
	}
	
	public long getEndTime() {
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
	
	public long getSourceID(){
		return ID_from;
	}
	
	public long getDestID(){
		return ID_to;
	}
	
	public void setStartConditions( Node node ) {
		area.setLocation( node.getCenterX() + width/32, node.getCenterY() + height/30 );
		distance = 0;
		timer = 0;
		hasFinished = false;
		active = true;
	}
	
	public boolean linkCrossed() {
		return distance > linkLenght;
	}
	
	public void update( final GameContainer gc, final int frames, final boolean animate ) {		
		mouseX = gc.getInput().getMouseX();
		mouseY = gc.getInput().getMouseY();
		
		timer = timer + frames;
		
		if (animate && active) {
			distance = distance + speedX;
			area.setLocation( area.getX() + speedX, area.getY() );
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
		g.rotate( startX, startY, angle );
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
