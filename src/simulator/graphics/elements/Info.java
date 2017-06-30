package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Info
{
	private Rectangle area;
	private boolean visible = false;
	
	private String infos;
	private final float OFFSET = 5f;
	
	public Info() {}
	
	public void setAttributes( final Graphics g, final String info, final float x, final float y ) {
		Font f = g.getFont();
		infos = info;
		area = new Rectangle( x - OFFSET, y - OFFSET, f.getWidth( info ) + 2 * OFFSET, f.getHeight( info ) + 2 * OFFSET );
        visible = true;
	}
	
	public void setVisible( final boolean flag ) {
	    visible = flag;
	}
	
	public void render( final Graphics g )
	{
	    if (visible) {
    		g.setColor( Color.lightGray );
    		g.fill( area );
    		
    		g.setColor( Color.black );
    		g.draw( area );
    		
    		g.drawString( infos, area.getX() + OFFSET, area.getY() + OFFSET );
	    }
	}
}