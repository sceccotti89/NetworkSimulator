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
		
		this.startTime = startTime;
		this.endTime = endTime;
		
		widthInfo  = width/7;
		heightInfo = height/13;
		
		infos = new Rectangle( 0, 0, widthInfo, heightInfo );
		
		drawInfo = false;
		
		init();
	}
	
	public void init()
	{
		area = new Rectangle( source.getCenterX() + source.getRay(), source.getCenterY() - height/120, width/80, height/60 );
		distance = 0;
		timer = 0;
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
	
	public void setPosition( final long time )
	{
	    active = true;
		timer = Math.max( 0, time - startTime );
		if (timer == 0) {
			distance = 0;
		} else {
			distance = (float) ((((double) (time - startTime)) / ((double) (endTime - startTime))) * linkLenght);
		}
		
		area.setX( source.getCenterX() + source.getRay() + distance );
	}
	
	public void setAngle() {
		angle = source.getAngle( dest.getNodeID() );
	}
	
	public void setSpeed( final int frames ) {
		speedX = (linkLenght / (endTime - startTime)) * frames;
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
	
	public void update( final GameContainer gc, final float time )
	{
	    if (active) {
    	    if (time >= startTime) {
        		int mouseX = gc.getInput().getMouseX();
        		int mouseY = gc.getInput().getMouseY();
        		
        		if (time >= endTime) {
        		    setActive( false );
        		    return;
        		}
        		
    			distance = distance + speedX;
    			area.setLocation( area.getX() + speedX, area.getY() );
        		
    			drawInfo = area.contains( mouseX, mouseY );
        		if (drawInfo) {
        			float offset = gc.getWidth()/80;
        			infos.setLocation( mouseX + offset, mouseY - offset );
        		}
    	    }
	    }
	}
	
	public void draw( final long time, final Graphics g )
	{
		if (time < startTime || time > endTime)
			return;
		
		g.rotate( source.getCenterX(), source.getCenterY(), angle );
		
		float offset = height/30;
		if (angle == -180) offset *= -1;
		
		area.setY( area.getY() + offset );
		g.setColor( color );
		g.fill( area );
		g.resetTransform();
		area.setY( area.getY() - offset );
		
		if (drawInfo) {
			Font f = g.getFont();
			String info = "Source node = " + source.getNodeID() + "\nDestination node = " + dest.getNodeID();
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
	
	@Override
	public String toString() {
	    return "Source: " + source.getNodeID() + ", Destination: " + dest.getNodeID();
	}
}
