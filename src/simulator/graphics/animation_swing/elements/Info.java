package simulator.graphics.animation_swing.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class Info
{
	private Rectangle area;
	private boolean visible = false;
	
	private String infos;
	private final float OFFSET = 5f;
	
	
	
	public Info() {
	    
	}
	
	public void setAttributes( final Graphics2D g, final String info, final float x, final float y )
	{
	    Rectangle2D bounds = g.getFontMetrics().getStringBounds( info, g );
        double width  = bounds.getWidth();
        double height = bounds.getHeight();
		infos = info;
		area = new Rectangle( (int) (x - OFFSET), (int) (y - OFFSET),
		                      (int) (width + 2 * OFFSET), (int) (height + 2 * OFFSET) );
        setVisible( true );
	}
	
	public void setVisible( final boolean flag ) {
	    visible = flag;
	}
	
	public void render( final Graphics2D g )
	{
	    if (visible) {
    		g.setColor( Color.lightGray );
    		g.fill( area );
    		
    		g.setColor( Color.black );
    		g.draw( area );
    		
    		Rectangle2D bounds = g.getFontMetrics().getStringBounds( infos, g );
    		double height = bounds.getHeight();
    		g.drawString( infos, (int) (area.getX() + OFFSET), (int) (area.getY() + height + OFFSET) );
	    }
	}
}