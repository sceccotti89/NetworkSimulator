/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animation_swing.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

public class TimeSlider
{
    private int orientation;
    private double min;
    private double max;
    private double value;
    
    private Rectangle area;
    private Ellipse2D cursor;
    private Rectangle info;
    
    private Point mouse;
    private boolean pressed = false;
    
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
        
        info = new Rectangle();
        
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
    
    private double computeValue( final double x ) {
        return ((x - area.getX()) / area.getWidth()) * (max - min);
    }
    
    public boolean isPressed() {
        return pressed;
    }
    
    public boolean mouseMoved( final MouseEvent e )
    {
        mouse = e.getPoint();
        if (pressed) {
            double value = computeValue( mouse.getX() );
            setValue( value );
        }
        return area.contains( mouse );
    }
    
    public boolean mousePressed( final MouseEvent e )
    {
        mouseMoved( e );
        
        if (area.contains( mouse )) {
            pressed = true;
            double value = computeValue( mouse.getX() );
            setValue( value );
            return true;
        }
        
        pressed = false;
        return false;
    }
    
    public void mouseReleased() {
        pressed = false;
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
            double val = (max - min) * (offsetX / area.getWidth());
            if (orientation == VERTICAL) {
                
            } else {
                Rectangle2D sBounds = g.getFontMetrics().getStringBounds( val + "", g );
                info.setBounds( (int) mouse.getX(), (int) area.getMaxY(), 100, 30 );
                g.setColor( Color.LIGHT_GRAY );
                g.fill( info );
                g.setColor( Color.BLACK );
                g.draw( info );
                g.drawString( val + "", (float) info.getX(), (float) (info.getCenterY() + sBounds.getHeight()/2) );
            }
        }
    }
}