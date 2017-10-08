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
    
    private static final int CURSOR_WIDTH  = 15;
    private static final int CURSOR_OFFSET = 5;
    
    private static final int RADIUS = 10;
    
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL   = 1;
    
    public TimeSlider( final int orientation, final double min, final double max, final double value )
    {
        this.orientation = orientation;
        this.min = min;
        this.max = max;
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
        // TODO sistemare
        double offsetX = this.value - value;
        //cursor.translate( , 0 );
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
            int offsetX = (int) (mouse.getX() - cursor.getX());
            Rectangle bounds = cursor.getBounds();
            bounds.translate( offsetX - RADIUS, 0 );
            cursor.setFrame( bounds );
            setValue( (max - min) * offsetX );
            return true;
        }
        
        return false;
    }
    
    public void draw( final Graphics2D g )
    {
        if (orientation == VERTICAL) {
            
        } else {
            // TODO per adesso disegnare qui (che e' quello che mi serve).
            g.setColor( Color.RED );
            g.fill( area );
            g.setColor( Color.BLACK );
            g.draw( area );
            
            g.setColor( Color.WHITE );
            g.fill( cursor );
            g.setColor( Color.BLACK );
            g.draw( cursor );
            
            // TODO in base alla posizione del mouse scrivere il valore corrente
            if (area.contains( mouse )) {
                double offsetX = mouse.getX() - area.getX();
                double time = (max - min) * offsetX;
                //TODO inserire il valore
                
            }
        }
        
        
    }
}