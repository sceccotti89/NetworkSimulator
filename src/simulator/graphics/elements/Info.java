package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Info
{
	private Rectangle area;
	
	private String infos;
	
	private Color color;
	
	public Info( Color color, String info ) {
		this.color = color;
	}
	
	public void setAttributes( Graphics g, String info ) {
		Font f = g.getFont();
		infos = info;
		area = new Rectangle( 0, 0, f.getWidth( info ), f.getHeight( info )   );
	}
	
	public void render( Graphics g, float x, float y, float angle ) {
		g.rotate( -angle, area.getCenterX(), area.getCenterY() );
		g.setColor( color );
		g.fill( area );
		
		g.setColor( Color.black );
		g.draw( area );
		
		g.drawString( infos, x, y );
		g.rotate( angle, area.getCenterX(), area.getCenterY() );
	}
}
