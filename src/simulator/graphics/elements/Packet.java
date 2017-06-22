package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Packet implements Comparable<Packet>
{
	private Color color;
	
	private Rectangle area;
	
	private boolean hasFinished;
	
	private long startTime, endTime;
	
	private boolean active;
	
	private float speedX;
	
	private float angle;
	
	private float widthInfo, heightInfo;
	private Rectangle infos;
	private boolean drawInfo;
	
	private int width, height;
	
	private float linkLenght;
	
	private float distance;
	
	private int mouseX, mouseY;
	
	private long timer;
	
	private Node source, dest;
	
	public Packet( final Node source, final Node dest,
				   final Color color,
				   final long startTime, final long endTime,
				   final int width, final int height ) {
		
		this.source = source;
		this.dest = dest;
		this.color = color;
		this.height = height;
		this.width = width;
		
		//area = new Rectangle( source.getCenterX() + width/32, source.getCenterY(), width/80, height/60 );
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		widthInfo  = width/7;
		heightInfo = height/13;
		
		infos = new Rectangle( 0, 0, widthInfo, heightInfo );
		
		drawInfo = false;
		
		init();
	}
	
	public void init() {
		area = new Rectangle( source.getCenterX() + source.getRay(), source.getCenterY() - height/120, width/80, height/60 );
		System.out.println( "AREA.X = " + area.getX() + " AREA.Y = " + area.getY() );
		//area.setLocation( source.getCenterX() + width/32, source.getCenterY() + height/30 );
		distance = 0;
		timer = 0;
		hasFinished = false;
		active = true;
	}
	
	public float getSpeedX() {
		return speedX;
	}
	
	public void setLinkLenght() {
		linkLenght = source.getLinkLenght( dest.getNodeID() ) - 2*source.getRay();
	}
	
	public float getLinkLenght() {
		return linkLenght;
	}
	
	/**metodo per riattivare i pacchetti nel caso di click nella barra*/
	public void setConditions( final long time ) {
		timer = time - startTime;
		if (timer <= 0) {
			distance = 0;
		} else {
			distance = ((time - startTime) / (endTime - startTime)) * linkLenght;
		}
		
		area.setLocation( source.getCenterX() + width/32 + distance, source.getCenterY() + height/30 );
		active = true;
	}
	
	public void setAngle() {
		angle = source.getAngle( dest.getNodeID() );
		System.out.println( "ANGLE = " + angle );
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
	
	public void setFinished( boolean val ) {
		hasFinished = val;
	}
	
	public boolean getFinished() {
		return hasFinished;
	}
	
	public void setSourceNode( final Node source ) {
		this.source = source;
	}
	
	public void setDestNode( final Node dest ) {
		this.dest = dest;
	}
	
	public Rectangle getArea(){
		return area;
	}
	
	public void setColor( Color color ){
		this.color = color;
	}
	
	public Node getSourceNode(){
		return source;
	}
	
	public Node getDestNode(){
		return dest;
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
		g.rotate( source.getCenterX(), source.getCenterY(), angle );
		if () ;
		area.setY( area.getY() + height/30 * Math.abs( angle )/angle );
		g.setColor( color );
		g.fill( area );
		g.resetTransform();
		area.setY( area.getY() - height/30 * Math.abs( angle )/angle );
		
		if (drawInfo) {
			Font f = g.getFont();
			String info = "ID_From = " + source.getNodeID() + "\n" + "ID_To = " + dest.getNodeID();
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
