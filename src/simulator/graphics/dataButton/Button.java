package simulator.graphics.dataButton;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;

public class Button
{
	/* area del bottone rettangolare */
	protected Shape rect;
	/* nome del bottone */
	protected String name;
	/* determina se il bottone e' stato premuto */
	protected boolean pressed = false;

	public Button()
		{}

	/** restituisce il nome del bottone
	 * @return name - il nome del bottone
	*/
	public String getName()
		{ return name; }
	
	public boolean checkClick( int x, int y )
		{ return rect.contains( x, y ); }

	/** restituisce lo stato del bottone
	 * @return TRUE se il bottone e' premuto, FALSE altimenti
	*/
	public boolean isPressed()
		{ return pressed; }

	/** modifica lo stato (premuto/non premuto) del bottone
	 * @param val - bottone premuto o no
	*/
	public void setPressed( boolean val )
		{ pressed = val; }

	/** assegna una nuova posizione Y
	 * @param y - la nuova coordinata Y
	*/
	public void setY( float y )
		{ rect.setY( y ); }

	/** assegna una nuova posizione X
	 * @param x - la nuova coordinata X
	*/
	public void setX( float x )
		{ rect.setX( x ); }
	
	/** restituisce la coordinata x del bottone
	 * @return rect.getX() - il valore della coordinata x del bottone
	*/
	public float getX()
		{ return rect.getX(); }
	
	/** restituisce la coordinata y del bottone
	 * @return name - il valore della coordinata x del bottone
	*/
	public float getY()
		{ return rect.getY(); }

	/** restituisce l'area occupata dal bottone
	 * @return rect - area del bottone
	*/
	public Rectangle getRect()
		{ return (Rectangle) rect; }

	/** determina se i punti x e y sono all'interno dell'area
	 * @param x - la coordinata X del mouse
	 * @param y - la coordinata Y del mouse
	 * 
	 * @return TRUE se e' all'interno, FALSE altrimenti
	*/
	public boolean contains( int x, int y )
		{ return rect.contains( x, y ); }

	/** disegna il bottone
	 * @param g - il contesto grafico
	*/
	protected void draw( Graphics g )
		{
			g.setLineWidth( 1.f );
			float x = rect.getX(), y = rect.getY(), maxX = rect.getMaxX(), maxY = rect.getMaxY();
	
			g.setColor( (pressed) ? Color.darkGray : Color.lightGray );
			g.drawLine( x, y, maxX, y );
			g.drawLine( x, y, x, maxY );
			g.setColor( (pressed) ? Color.gray : Color.white );
			g.drawLine( x + 1, y + 1, maxX, y + 1 );
			g.drawLine( x + 1, y + 1, x + 1, maxY );
			g.setColor( (pressed) ? Color.lightGray : Color.darkGray );
			g.drawLine( maxX - 1, maxY - 1, x + 2 * 1, maxY - 1 );
			g.drawLine( maxX - 1, maxY - 1, maxX - 1, y + 2 * 1 );
			g.setColor( (pressed) ? Color.white : Color.gray );
			g.drawLine( maxX, maxY, x + 1, maxY );
			g.drawLine( maxX, maxY, maxX, y + 1 );
		}
}