package simulator.graphics_swing.elements;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Info
{
	private Rectangle area;
	private boolean visible = false;
	
	private String infos;
	private final float OFFSET = 5f;
	
	public Info() {}
	
	public void setAttributes( final Graphics2D g, final String info, final float x, final float y )
	{
	    FontMetrics font = g.getFontMetrics();
        double width  = font.stringWidth( info );
        double height = font.getHeight();
		infos = info;
		area = new Rectangle( (int) (x - OFFSET), (int) (y - OFFSET),
		                      (int) (width + 2 * OFFSET), (int) (height + 2 * OFFSET) );
        visible = true;
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
    		
    		g.drawString( infos, (int) (area.getX() + OFFSET), (int) (area.getY() + OFFSET) );
	    }
	}
}