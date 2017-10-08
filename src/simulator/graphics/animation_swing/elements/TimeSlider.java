/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animation_swing.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

public class TimeSlider
{
    private int orientation;
    private double min;
    private double max;
    private double value;
    
    private Rectangle area;
    private Ellipse2D cursor;
    
    private Point mouse;
    
    private static final int RADIUS = 10;
    
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL   = 1;
    
    public TimeSlider( final int orientation, final double min, final double max, final double value )
    {
        this.orientation = orientation;
        this.min = min;
        this.max = max;
        area = new Rectangle();
        cursor = new Ellipse2D.Double();
        setValue( value );
        
        mouse = new Point( -1, -1 );
    }
    
    public void setBounds( final float x, final float y, final float width, final float height )
    {
        area = new Rectangle( (int) x, (int) y, (int) width, (int) height );
        cursor = new Ellipse2D.Double( x - RADIUS, area.getCenterY() - RADIUS, 2 * RADIUS, 2 * RADIUS );
    }
    
    public void setValue( final double value )
    {
        int x = (int) (area.getX() + ((value/(max-min)) * area.getWidth()));
        Rectangle bounds = cursor.getBounds();
        bounds.setLocation( x - RADIUS, bounds.y );
        cursor.setFrame( bounds );
        this.value = value;
    }
    
    public double getValue() {
        return value;
    }
    
    public double getMaximum() {
        return max;
    }
    
    public boolean checkMouseClick( final Point p )
    {
        mouse = p;
        if (area.contains( p )) {
            double value = ((mouse.getX() - area.getX()) / area.getWidth()) * (max - min);
            setValue( value );
            return true;
        }
        
        return false;
    }
    
    public void draw( final Graphics2D g )
    {
        g.setColor( Color.RED );
        g.fill( area );
        g.setColor( Color.BLACK );
        g.draw( area );
        
        g.setColor( Color.WHITE );
        g.fill( cursor );
        g.setColor( Color.BLACK );
        g.draw( cursor );
        
        if (area.contains( mouse )) {
            double offsetX = mouse.getX() - area.getX();
            double time = (max - min) * (offsetX / area.getWidth());
            //TODO inserire il valore
            if (orientation == VERTICAL) {
                
            } else {
                
            }
        }
    }
}