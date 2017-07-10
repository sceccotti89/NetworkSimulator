package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.interfaces.AnimationManager;
import simulator.graphics.interfaces.NetworkDisplay;

public class Packet implements Comparable<Packet>
{
    private final Color color;
    
    private Rectangle area;
    private Polygon areaRotated;
    
    private final long startTime, endTime;
    
    private boolean active;
    
    private float speed;
    
    private float angle;
    
    private final int width, height;
    
    private float linkLenght, distance;
    
    private Node source, dest;
    
    private float startX, startY;

    private final int offset, type;
    
    private final int limit = 1000000;

	private String measure;

	private int mouseX, mouseY;
    
    public Packet( final Node source, final Node dest,
                   final Color color,
                   final long startTime, final long endTime,
                   final int width, final int height,
                   final int type) {
        
        this.source = source;
        this.dest = dest;
        this.color = color;
        this.height = height;
        this.width = width;
        
        this.startTime = startTime;
        this.endTime = endTime;
        
        this.type = type;
        
        offset = width/80;
        
        init( source, dest, startTime );
    }
    
    public void init( final Node source, final Node dest, final long time )
    {
    	this.dest = dest;
    	this.source = source;
	
        angle = source.getAngle( dest.getNodeID() );
        
        linkLenght = source.getLinkLenght( dest.getNodeID() ) - 2 * source.getRay();
        
        startX = source.getCenterX() + source.getRay();
        startY = source.getCenterY() - height/120;
        area = new Rectangle( startX, startY, width/80, height/60 );
        
        setPosition( time );
    }
    
    public Node getNodeSource() {
    	return source;
    }
    
    public Node getNodeDest() {
    	return dest;
    }
    
    /**set the position of the packet basing on the time of the simulation*/
    public void setPosition( final long time )
    {
        active = true;
        distance = (float) ((((double) time - startTime) / (endTime - startTime)) * linkLenght);
        if (distance < 0) distance = 0;
        
        area.setX( startX + distance );
        rotatePacket();
    }
    
    public void setSpeed() {
        speed = (linkLenght / (endTime - startTime)) * AnimationManager.frames;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive( final boolean val ) {
        active = val;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    private Point worldToView( final float x, final float y, float angle ) {
    	angle = (float) (angle * Math.PI/180.f);
    	return new Point( (float) (x * Math.cos( angle ) - y * Math.sin( angle )), (float) (x * Math.sin( angle ) + y * Math.cos( angle )) );
    }
    
    private void rotatePacket()
    {
        float offset = height/30;
        if (angle >= 90 || angle <= -90 || angle == -180) offset *= -type;
        area.setY( area.getY() + offset );
        
        final float centerX = source.getCenterX(), centerY = source.getCenterY();
        Point p1 = worldToView( area.getX() - centerX,    area.getY() - centerY, angle );
        Point p2 = worldToView( area.getMaxX() - centerX, area.getY() - centerY, angle );
        Point p3 = worldToView( area.getMaxX() - centerX, area.getMaxY() - centerY, angle );
        Point p4 = worldToView( area.getX() - centerX,    area.getMaxY() - centerY, angle );
        areaRotated = new Polygon( new float[]{ p1.getX() + centerX, p1.getY() + centerY,
                                                p2.getX() + centerX, p2.getY() + centerY,
                                                p3.getX() + centerX, p3.getY() + centerY,
                                                p4.getX() + centerX, p4.getY() + centerY } );
        area.setY( area.getY() - offset );
    }
    
    public void setMeasure( final String measure ) {
    	this.measure  = measure;
    }
    
    private String setInfo( long time, final String measure ) {
    	long decimal = time;
    	if (measure.equals( "TIME" )) {
    		time = time / limit;
    	} else {
    		return time + "탎";
    	}
    	
    	long h = time/3600, m = (time - h*3600)/60, s = time - h*3600 - m*60,
    		 ms = (decimal - (h*3600 + m*60 + s) * limit) / 1000, ns = (decimal - (h*3600 + m*60 + s) * limit) - ms * 1000;
    	
    	String info = h + "h:";
    	if (m < 10) {
    		info = info + "0" + m + "m:";
    	} else {
    		info = info + m + "m:";
    	}
    	
    	if (s < 10) {
    		info = info + "0" + s + "s:";
    	} else {
    		info = info + s + "s:";
    	}
    	
    	if (ms < 10) {
			info = info + "00" + ms + "ms:";
    	} else if (ms < 100) {
    		info = info + "0" + ms + "ms:";
    	} else {
    		info = info + ms + "ms:";
    	}
    	
    	if (ns < 10) {
			info = info + "00" + ns + "탎";
    	} else if (ns < 100) {
    		info = info + "0" + ns + "탎";
    	} else {
    		info = info + ns + "탎";
    	}
    	
    	return info;
    }
    
    public void update( final GameContainer gc, final long time, final boolean update )
    {
    	mouseX = gc.getInput().getMouseX();
    	mouseY = gc.getInput().getMouseY();
    	
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
        	NetworkDisplay.info.setAttributes( gc.getGraphics(), toString(), mouseX + offset, mouseY + offset );
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
    public int compareTo( final Packet packet ) {
        if (startTime < packet.startTime) return -1;
        if (startTime > packet.startTime) return 1;
        return 0;
    }
    
    @Override
    public String toString() {
        return "start  = " + setInfo( startTime, measure ) + "\n" +
               "end    = " + setInfo( endTime, measure ) + "\n" +
               "source = " + source.getNodeID() + "\n" +
               "dest   = " + dest.getNodeID();
    }
}