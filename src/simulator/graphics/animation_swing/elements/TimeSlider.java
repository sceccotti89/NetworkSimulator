/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animation_swing.elements;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

public class TimeSlider
{
    private JPanel panel;
    
    private int orientation;
    private long min;
    private long max;
    private long value;
    
    private Rectangle area;
    private Ellipse2D cursor;
    private Rectangle info;
    
    private Point mouse;
    private boolean pressed = false;
    
    private static final Cursor HAND_CURSOR = new Cursor( Cursor.HAND_CURSOR );
    
    private static final int RADIUS = 10;
    
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL   = 1;
    
    public TimeSlider( final JPanel panel, final int orientation, final long min, final long max, final long value )
    {
        this.panel = panel;
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
        area.setBounds( (int) x, (int) y, (int) width, (int) height );
        cursor = new Ellipse2D.Double( x - RADIUS, area.getCenterY() - RADIUS, RADIUS * 2, RADIUS * 2 );
    }
    
    public void setValue( final double value )
    {
        int x = (int) (area.getX() + ((value/(max-min)) * area.getWidth()));
        Rectangle bounds = cursor.getBounds();
        bounds.setLocation( x - RADIUS, bounds.y );
        cursor.setFrame( bounds );
        this.value = (long) value;
    }
    
    public double getValue() {
        return value;
    }
    
    public void setMaximum( final long max ) {
        this.max = max;
    }
    
    public double getMaximum() {
        return max;
    }
    
    public long getRange() {
        return max - min;
    }
    
    private long computeValue( final double x )
    {
        double value = ((x - area.getX()) / area.getWidth()) * (max - min);
        value = Math.max( min, value );
        value = Math.min( value, max );
        double upper = Math.ceil( value );
        if (upper - value < 0.5) {
            value = upper;
        }
        return (long) value;
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
        if (area.contains( mouse )) {
            panel.setCursor( HAND_CURSOR );
            return true;
        } else {
            panel.setCursor( Cursor.getDefaultCursor() );
            return false;
        }
    }
    
    public boolean mousePressed( final MouseEvent e )
    {
        mouseMoved( e );
        
        if (area.contains( mouse )) {
            pressed = true;
            double value = computeValue( mouse.getX() );
            setValue( value );
            return true;
        } else if (cursor.contains( mouse )){
            pressed = true;
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
        g.setColor( Color.GRAY );
        g.fill( area );
        g.setColor( Color.RED );
        g.fillRect( area.x, area.y, (int) (cursor.getCenterX() - area.getX()), area.height );
        g.setColor( Color.BLACK );
        g.draw( area );
        
        final float OFFSET_X = 3, OFFSET_Y = 1;
        
        if (area.contains( mouse )) {
            g.setColor( Color.WHITE );
            g.fill( cursor );
            g.setColor( Color.BLACK );
            g.draw( cursor );
            
            double offsetX = mouse.getX() - area.getX();
            double info_value = (max - min) * (offsetX / area.getWidth());
            double upper = Math.ceil( info_value );
            if (upper - info_value < 0.5) {
                info_value = (long) upper;
            }
            long val = (long) info_value;
            if (orientation == VERTICAL) {
                
            } else {
                Rectangle2D sBounds = g.getFontMetrics().getStringBounds( val + "", g );
                info.setBounds( (int) (mouse.getX() - sBounds.getWidth()/2 - OFFSET_X),
                                (int) area.getY() - 20, (int) (sBounds.getWidth() + OFFSET_X * 2),
                                (int) (sBounds.getHeight() + OFFSET_Y * 2) );
                g.setColor( Color.LIGHT_GRAY );
                g.fill( info );
                g.setColor( Color.BLACK );
                g.draw( info );
                g.drawString( val + "", (float) (info.getX() + OFFSET_X), (float) (info.getCenterY() + sBounds.getHeight()/2) );
            }
        } else {
            if (pressed) {
                g.setColor( Color.WHITE );
                g.fill( cursor );
                g.setColor( Color.BLACK );
                g.draw( cursor );
            }
        }
        
        Rectangle2D sBounds = g.getFontMetrics().getStringBounds( value + "/" + max, g );
        g.setColor( Color.BLACK );
        g.drawString( value + "/" + max,
                     (float) (area.getCenterX() - sBounds.getWidth()/2),
                     (float) (area.getMaxY() + sBounds.getHeight() + OFFSET_Y) );
    }
}