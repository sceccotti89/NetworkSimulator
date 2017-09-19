
package simulator.graphics.animation_swing.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;

import simulator.graphics.animator_swing.interfaces.NetworkDisplay;

public class Link
{
    private float angle;
    
    private double bandwidth;
    private long delay;
    
    private Rectangle area;
    private Polygon areaRotated;
        
    private final float offset;
    
    private float lenght;
    
    private Node source, dest;
    
    private String type;
    
    public Link( final Node source, final Node dest,
                 final double bandwidth, final long delay,
                 float angle, final int width, final int height,
                 final String type )
    {
        this.source = source;
        this.dest = dest;
        this.angle = angle;
        this.type = type;
        
        this.bandwidth = bandwidth;
        this.delay = delay;
        
        lenght = calculateLenght( source.getCenterX(), source.getCenterY(), dest.getCenterX(), dest.getCenterY() );

        offset = width/100;
        area = new Rectangle( (int) source.getCenterX(), (int) (source.getCenterY() - offset), (int) lenght, (int) height*10/375 );
        
        rotateLink();
    }
    
    private Point worldToView( final double x, final double y, final double angle ) {
        double teta = (double) (angle * Math.PI/180.f);
        return new Point( (int) (x * Math.cos( teta ) - y * Math.sin( teta )),
                          (int) (x * Math.sin( teta ) + y * Math.cos( teta )) );
    }
    
    private void rotateLink()
    {
    	final float centerX = source.getCenterX(), centerY = source.getCenterY();
        Point p1 = worldToView( area.getX() - centerX,    area.getY() - centerY,    angle );
        Point p2 = worldToView( area.getMaxX() - centerX, area.getY() - centerY,    angle );
        Point p3 = worldToView( area.getMaxX() - centerX, area.getMaxY() - centerY, angle );
        Point p4 = worldToView( area.getX() - centerX,    area.getMaxY() - centerY, angle );
        areaRotated = new Polygon( new int[]{ (int) (p1.getX() + centerX), (int) (p2.getX() + centerX),
                                              (int) (p3.getX() + centerX), (int) (p4.getX() + centerX) },
                                   new int[]{ (int) (p1.getY() + centerY), (int) (p2.getY() + centerY),
                                              (int) (p3.getY() + centerY), (int) (p4.getY() + centerY) },
                                   4 );
    }
    
    public void update( final Graphics2D g, final Point mouse )
    {
        final int mouseX = mouse.x;
        final int mouseY = mouse.y;
        
    	if (source.getArea().contains( mouseX, mouseY ) ||
    	    dest.getArea().contains( mouseX, mouseY )) {
    		return;
    	}
    	
    	if (areaRotated.contains( mouseX, mouseY )) {
    	    NetworkDisplay.info.setAttributes( g, toString(), mouseX + offset, mouseY + offset );
        }
    }
    
    public void setPosition( final Node node, final float angle ) {
    	this.source = node;

    	lenght = calculateLenght( source.getCenterX(), source.getCenterY(), dest.getCenterX(), dest.getCenterY() );
    	area = new Rectangle( (int) source.getCenterX(), (int) (source.getCenterY() - offset), (int) lenght, (int) area.getHeight() );
    	
    	this.angle = angle;
    	rotateLink();
    }
    
    public Node getSourceNode() {
        return source;
    }
    
    public Node getDestNode() {
        return dest;
    }
    
    public float calculateLenght( float x1, float y1, float x2, float y2 ) {
        return (float) Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) );
    }
    
    public float getLenght() {
        return lenght;
    }
    
    public float getAngle() {
        return angle;
    }
    
    public String getType() {
    	return type;
    }
    
    public void draw( final Graphics2D g )
    {
        g.setColor( Color.BLACK );
        g.draw( areaRotated );
    }
    
    @Override
    public String toString() {
        return "bandwidth = " + bandwidth + "Mb/s, " +
               "delay = " + delay + "ms";
    }
}