package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Time
{
	private final Rectangle area;
	private final String time;
	
	private final float width;
	
	private boolean selected = true;
	
	public Time( final float x, final float y, final float width, final float height, final String time )
	{
		this.time = time;
		this.width = width;
		
		area = new Rectangle( x, y, width, height );
	}
	
	public boolean checkClick( final float mouseX, final float mouseY ) {
		return area.contains( mouseX, mouseY );
	}
	
	public void setSelected(  ) {
		selected = !selected;
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void render( Graphics g ) {
		if (!selected) {
			g.setColor( Color.white );
		} else {
			g.setColor( Color.green );
		}

		g.fill( area );
		
		g.setColor( Color.white );
		final float heightF = g.getFont().getHeight( time );
		g.drawString( time, area.getMaxX() + width/2, area.getCenterY() - heightF/2 );
	}
}