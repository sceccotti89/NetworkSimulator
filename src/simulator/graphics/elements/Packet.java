package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.interfaces.NetworkDisplay;

public class Packet implements Comparable<Packet>
{
    private final Color color;
    
    private Rectangle area;
    private Polygon areaRotated;
    
    private final long startTime, endTime;
    
    private boolean active;
    
    private float speed;
    
    private final float angle;
    
    private final int width, height;
    
    private float linkLenght, distance;
    
    private final Node source, dest;

    private final int offset;
    
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
        
        offset = width/80;
        
        angle = source.getAngle( dest.getNodeID() );
        System.out.println( "ANGLE = " + angle );
        
        linkLenght = source.getLinkLenght( dest.getNodeID() ) - 2 * source.getRay();
        
        init();
    }
    
    public void init()
    {
        area = new Rectangle( source.getCenterX() + source.getRay(), source.getCenterY() - height/120, width/80, height/60 );
        rotatePacket();
        distance = 0;
        active = true;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public void setPosition( final long time )
    {
        active = true;
        distance = (float) ((((double) time - startTime) / (endTime - startTime)) * linkLenght);
        
        area.setX( source.getCenterX() + source.getRay() + distance );
        rotatePacket();
    }
    
    public void setSpeed( final int frames ) {
        speed = (linkLenght / (endTime - startTime)) * frames;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive( boolean val ) {
        active = val;
    }
    
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int compareTo( Packet packet ) {
        if (startTime < packet.startTime) return -1;
        if (startTime > packet.startTime) return 1;
        return 0;
    }
    
    private Point worldToView( final float x, final float y, float angle ) {
    	angle = (float) (angle * Math.PI/180.f);
    	return new Point( (float) (x * Math.cos( angle ) - y * Math.sin( angle )), (float) (x * Math.sin( angle ) + y * Math.cos( angle )) );
    }
    
    private void rotatePacket()
    {
        float offset = height/30;
        if (angle >= 90 || angle <= -90 || angle == -180) offset *=-1;
        area.setY( area.getY() + offset );
        
        Point p1 = worldToView( area.getX() - source.getCenterX(),    area.getY() - source.getCenterY(), angle );
        Point p2 = worldToView( area.getMaxX() - source.getCenterX(), area.getY() - source.getCenterY(), angle );
        Point p3 = worldToView( area.getMaxX() - source.getCenterX(), area.getMaxY() - source.getCenterY(), angle );
        Point p4 = worldToView( area.getX() - source.getCenterX(),    area.getMaxY() - source.getCenterY(), angle );
        areaRotated = new Polygon( new float[]{ p1.getX() + source.getCenterX(), p1.getY() + source.getCenterY(),
                                                p2.getX() + source.getCenterX(), p2.getY() + source.getCenterY(),
                                                p3.getX() + source.getCenterX(), p3.getY() + source.getCenterY(),
                                                p4.getX() + source.getCenterX(), p4.getY() + source.getCenterY() } );
        area.setY( area.getY() - offset );
    }
    
    public void update( final GameContainer gc, final long time, final boolean update )
    {
    	int mouseX = gc.getInput().getMouseX();
    	int mouseY = gc.getInput().getMouseY();
    	
        if (time >= endTime) {
            active = false;
            return;
        }
        
        if (update) {
            distance = distance + speed;
            area.setLocation( area.getX() + speed, area.getY() );
            
            rotatePacket();
        }
        
    	if(areaRotated.contains( mouseX, mouseY )) {
        	NetworkDisplay.info.setAttributes( gc.getGraphics(), toString(), mouseX + offset, mouseY + offset, Color.magenta );
        }
    }
    
    public void render( final Graphics g, final long time )
    {
    	if (time < startTime || time > endTime)
            return;
        g.setColor( color );
        g.fill( areaRotated );
    }
    
    @Override
    public String toString() {
        return "startTime = " + startTime + "\n"
                + "endTime = " + endTime + "\n"
                + "source = " + source.getNodeID() + "\n"
                + "dest = " + dest.getNodeID();
    }
}