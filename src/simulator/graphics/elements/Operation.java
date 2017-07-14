package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;

public class Operation
{
	private Rectangle area;
	
	private String name;
	
	public Operation( final String name,  final float x, final float y, final float width, final float height )
	{
		this.name = name;
		
		area = new Rectangle( x, y, width + width/4, height );
	}
	
	public boolean checkCollision( final int mouseX, final int mouseY ) {
		return area.contains( mouseX, mouseY );
	}
	
	public boolean intersects( final float mouseX, final float mouseY ) {
	    return area.intersects( new Rectangle( mouseX, mouseY, 1, 1 ) );
	}
	
	public float getX() {
		return area.getX();
	}
	
	public float getY(){
	    return area.getY();
	}
	
	public float getMaxX(){
	    return area.getMaxX();
	}
	
	public float getMaxY() {
		return area.getMaxY();
	}
	
	public String getName() {
		return name;
	}
	
	public Shape getArea() {
	    return area;
	}
	
	public void update() {
		
	}
	
	public void render( Graphics g ) {
		g.setColor( Color.white );
		g.fill( area );
		g.setColor( Color.black );
		g.draw( area );
		
		int fWidth = g.getFont().getWidth( name ), fHeight = g.getFont().getHeight( name );
		g.drawString( name, area.getCenterX() - fWidth/2, area.getCenterY() - fHeight/2 );
	}
}
