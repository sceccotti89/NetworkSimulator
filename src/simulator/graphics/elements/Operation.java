package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class Operation
{
	private Rectangle area;
	
	private String name;
	
	public Operation( final String name,  final float x, final float y, final float width, final float height )
	{
		this.name = name;
		
		area = new Rectangle( x, y, width, height );
	}
	
	public boolean checkCollision( final int mouseX, final int mouseY ) {
		return area.contains( mouseX, mouseY );
	}
	
	public float getX() {
		return area.getX();
	}
	
	public float getMaxY() {
		return area.getMaxY();
	}
	
	public String getName() {
		return name;
	}
	
	public void update() {
		
	}
	
	public void render( Graphics g ) {
		g.setColor( Color.white );
		g.fill( area );
		g.setColor( Color.black );
		g.draw( area );
	}
}
