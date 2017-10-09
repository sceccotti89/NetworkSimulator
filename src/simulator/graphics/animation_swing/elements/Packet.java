
package simulator.graphics.animation_swing.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import simulator.graphics.animation_swing.interfaces.AnimationManager;
import simulator.graphics.animation_swing.interfaces.NetworkDisplay;

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
    private Link link;
    
    private float startX, startY;

    private final int offset, type;
    
    private final int limit = 1000000;

	private String measure;

	private int mouseX, mouseY;
	
	
    
    public Packet( final Node source, final Node dest, final Link link,
                   final Color color,
                   final long startTime, final long endTime,
                   final int width, final int height,
                   final int type)
    {
        this.source = source;
        this.dest = dest;
        this.link = link;
        this.color = color;
        this.height = height;
        this.width = width;
        
        this.startTime = startTime;
        this.endTime = endTime;
        
        this.type = type;
        
        offset = width/80;
        
        init( startTime );
    }
    
    public void init( final long time )
    {
        angle = source.calculateAngle( dest );
        
        linkLenght = link.getLenght() - 2 * source.getRay();
        
        startX = source.getCenterX() + source.getRay();
        startY = source.getCenterY() - height/120;
        area = new Rectangle( (int) startX, (int) startY, width/80, height/60 );
        
        setPosition( time );
    }
    
    /** Sets the position of the packet based on the time of the simulation. */
    public void setPosition( final long time )
    {
        active = true;
        distance = (float) ((((double) time - startTime) / (endTime - startTime)) * linkLenght);
        if (distance < 0) distance = 0;
        
        area.x = (int) (startX + distance);
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
    
    private Point worldToView( final double x, final double y, final double angle ) {
        double teta = (double) (angle * Math.PI/180.f);
        return new Point( (int) (x * Math.cos( teta ) - y * Math.sin( teta )),
                          (int) (x * Math.sin( teta ) + y * Math.cos( teta )) );
    }
    
    private void rotatePacket()
    {
        float offset = height/30;
        if (angle >= 90 || angle <= -90 || angle == -180) offset *= -type;
        area.y = (int) (area.getY() + offset);
        
        final float centerX = source.getCenterX();
        final float centerY = source.getCenterY();
        Point p1 = worldToView( area.getX() - centerX,    area.getY() - centerY, angle );
        Point p2 = worldToView( area.getMaxX() - centerX, area.getY() - centerY, angle );
        Point p3 = worldToView( area.getMaxX() - centerX, area.getMaxY() - centerY, angle );
        Point p4 = worldToView( area.getX() - centerX,    area.getMaxY() - centerY, angle );
        areaRotated = new Polygon( new int[]{ (int) (p1.getX() + centerX), (int) (p2.getX() + centerX),
                                              (int) (p3.getX() + centerX), (int) (p4.getX() + centerX) },
                                   new int[]{ (int) (p1.getY() + centerY), (int) (p2.getY() + centerY),
                                              (int) (p3.getY() + centerY), (int) (p4.getY() + centerY) },
                                   4 );
        area.y = (int) (area.getY() - offset);
    }
    
    public void setMeasure( final String measure ) {
    	this.measure  = measure;
    }
    
    private String getInfo( long time, final String measure )
    {
    	long decimal = time;
    	if (measure.equals( "TIME" )) {
    		time = time / limit;
    	} else {
    		return time + "us";
    	}
    	
    	long h = time/3600, m = (time - h*3600)/60, s = time - h*3600 - m*60,
    		 ms = (decimal - (h*3600 + m*60 + s) * limit) / 1000, us = (decimal - (h*3600 + m*60 + s) * limit) - ms * 1000;
    	
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
    	
    	if (us < 10) {
			info = info + "00" + us + "us";
    	} else if (us < 100) {
    		info = info + "0" + us + "us";
    	} else {
    		info = info + us + "us";
    	}
    	
    	return info;
    }
    
    public Packet clone() {
        return new Packet( source, dest, link, color, startTime, endTime, width, height, type );
    }
    
    public void update( final Graphics2D g, final long time, final boolean update, final Point mouse )
    {
    	if (time >= endTime) {
            active = false;
            return;
        }
    	
    	setPosition( time );
        
        if (update) {
            distance = distance + speed;
            area.setLocation( (int) (area.getX() + speed), (int) area.getY() );
            
            rotatePacket();
        }
        
        mouseX = mouse.x;
        mouseY = mouse.y;
        if(areaRotated.contains( mouseX, mouseY )) {
        	NetworkDisplay.info.setAttributes( g, toString(), mouseX + offset, mouseY + offset );
        }
    }
    
    public void draw( final Graphics2D g, final long time )
    {
    	if (time < startTime || time > endTime)
            return;
    	
    	float offset = height/30;
    	if (angle >= 90 || angle <= -90 || angle == -180) offset *= -type;
    	area.y = (int) (area.getY() + offset);
    	g.rotate( (angle * Math.PI) / 180d );
        g.setColor( color );
        //g.fill( areaRotated );
        g.fill( area );
        g.rotate( -((angle * Math.PI) / 180d) );
        area.y = (int) (area.getY() - offset);
    }

    @Override
    public int compareTo( final Packet packet ) {
        if (startTime < packet.startTime) return -1;
        if (startTime > packet.startTime) return 1;
        return 0;
    }
    
    @Override
    public String toString() {
        return "start  = " + getInfo( startTime, measure ) + "\n" +
               "end    = " + getInfo( endTime, measure ) + "\n" +
               "source = " + source.getID() + "\n" +
               "dest   = " + dest.getID();
    }
}