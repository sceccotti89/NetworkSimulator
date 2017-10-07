/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animator_swing.interfaces;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

public class TimeSlider
{
    private int orientation;
    private int min;
    private int max;
    private double value;
    
    private Rectangle area;
    private Rectangle cursor;
    
    private Point mouse;
    
    private static final int CURSOR_WIDTH  = 15;
    private static final int CURSOR_OFFSET = 5;
    
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL   = 1;
    
    public TimeSlider( final int orientation, final int min, final int max, final int value )
    {
        this.orientation = orientation;
        this.min = min;
        this.max = max;
        setValue( value );
    }
    
    public void setBounds( final float x, final float y, final float width, final float height )
    {
        area = new Rectangle( (int) x, (int) y, (int) width, (int) height );
        cursor = new Rectangle( (int) x, (int) y - CURSOR_OFFSET,
                                CURSOR_WIDTH, (int) height + CURSOR_OFFSET * 2 );
    }
    
    public void setValue( final double value )
    {
        cursor.translate( (int) (value - this.value), 0 );
        this.value = value;
    }
    
    public void checkMouseClick( final Point p )
    {
        mouse = p;
        if (area.contains( p )) {
            double offsetX = mouse.getX() - area.getX();
            setValue( offsetX );
        }
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
        }
        
        // TODO in base alla posizione del mouse scrivere il valore corrente
        if (area.contains( mouse )) {
            double offsetX = mouse.getX() - area.getX();
            double time = (max - min) * offsetX;
            //TODO inserire il valore
            
        }
    }
}