package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Polygon;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.interfaces.NetworkDisplay;

public class Link
{
    private Color color = Color.black;
    
    private float angle;
    
    private double bandwidth;
    
    private int delay;
    
    private Rectangle area;
    private Polygon areaRotated;
        
    private float offset;
    
    private float lenght;
    
    private Node source, dest;
    
    public Link( final Node source, final Node dest,
                 float x1, float y1, float x2, float y2,
                 float angle, final int width, final int height ) {
        
        this.source = source;
        this.dest = dest;
        this.angle = angle;
        
        lenght = calculateLenght( x1, y1, x2, y2 );

        offset = width/100;
        area = new Rectangle( x1, y1 - offset, lenght, height*10/375 );
        offset = width/80;
        
        rotateLink();
    }
    
    private Point worldToView( final float x, final float y, float angle ) {
        angle = (float) (angle * Math.PI/180.f);
        return new Point( (float) (x * Math.cos( angle ) - y * Math.sin( angle )), (float) (x * Math.sin( angle ) + y * Math.cos( angle )) );
    }
    
    private void rotateLink() {
    	final float centerX = source.getCenterX(), centerY = source.getCenterY();
        Point p1 = worldToView( area.getX() - centerX,    area.getY() - centerY, angle );
        Point p2 = worldToView( area.getMaxX() - centerX, area.getY() - centerY, angle );
        Point p3 = worldToView( area.getMaxX() - centerX, area.getMaxY() - centerY, angle );
        Point p4 = worldToView( area.getX() - centerX,    area.getMaxY() - centerY, angle );
        areaRotated = new Polygon( new float[]{ p1.getX() + centerX, p1.getY() + centerY,
                                                p2.getX() + centerX, p2.getY() + centerY,
                                                p3.getX() + centerX, p3.getY() + centerY,
                                                p4.getX() + centerX, p4.getY() + centerY } );
    }
    
    public void update( final GameContainer gc, final int mouseX, final int mouseY ) {
    	if (source.getArea().contains( mouseX, mouseY ) || dest.getArea().contains( mouseX, mouseY )) {
    		return;
    	}
    	
    	if (areaRotated.contains( mouseX, mouseY )) {
    	    NetworkDisplay.info.setAttributes( gc.getGraphics(), toString(), mouseX + offset, mouseY + offset );
        }
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
    
    public void render( final Graphics g ) {
        g.setColor( color );
        g.draw( areaRotated );
    }
    
    @Override
    public String toString() {
        return "bandwidth = " + bandwidth + "Mb/s, "
                + "delay = " + delay + "ms";
    }
}