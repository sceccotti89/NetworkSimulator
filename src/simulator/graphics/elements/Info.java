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
	//private static final float OFFSET = 10f;
	
	private Color color;
	
	public Info() {}
	
	public Info( Color color, String info ) {
		this.color = color;
	}
	
	public void setAttributes( Graphics g, String info, final float x, final float y ) {
		Font f = g.getFont();
		infos = info;
		area = new Rectangle( x, y, f.getWidth( info ), f.getHeight( info ) );
        visible = true;
	}
	
	public void setVisible( final boolean flag ) {
	    visible = flag;
	}
	
	public void render( final Graphics g )
	{
	    if (visible) {
    		g.setColor( color );
    		g.fill( area );
    		
    		g.setColor( Color.black );
    		g.draw( area );
    		
    		g.drawString( infos, area.getX(), area.getY() );
	    }
	}
}