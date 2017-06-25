package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Info
{
	private Rectangle area;
	
	private String infos;
	private static final float OFFSET = 10f;
	
	private Color color;
	
	public Info( Color color, String info ) {
		this.color = color;
	}
	
	public void setAttributes( Graphics g, String info ) {
		Font f = g.getFont();
		infos = info;
		area = new Rectangle( 0, 0, f.getWidth( info ), f.getHeight( info )   );
	}
	
	public void setPosition( final float x, final float y ) {
		area.setLocation( x, y );
	}
	
	public void render( final Graphics g ) {
		g.setColor( color );
		g.fill( area );
		
		g.setColor( Color.black );
		g.draw( area );
		
		g.drawString( infos, area.getX() + OFFSET, area.getY() + OFFSET );
	}
}