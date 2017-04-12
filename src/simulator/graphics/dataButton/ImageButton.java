
package simulator.graphics.dataButton;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

public class ImageButton extends Button
{
	/* determina se e' possibile premere il tasto */
	private boolean active = true;
	/* il colore del bottone */
	private Color c;
	
	// determina se il bottone e' cliccabile
	private boolean clickable;
	// l'indice del bottone
	private int index;
	/* lunghezza e altezza del bottone triangolare */
	float width, height;
	
	private Image image;
	private float widthImg, heightImg;

	/** crea un nuovo bottone rettangolare
	 * @param x - coordinata X
	 * @param y - coordinata Y
	 * @param name - il nome del bottone
	 * @throws SlickException 
	*/
    public ImageButton( float x, float y, float width, float height, String name, Color color, int index, GameContainer gc, Image image, float widthImg, float heightImg ) throws SlickException
		{
			super();
			
			c = color;

			this.name = name;
			this.width = width;
			this.height = height;
			buildButton( x, y, gc );
			
			clickable = true;
			
			this.index = index;
			
			this.image = image;
			
			this.widthImg = widthImg;
			this.heightImg = heightImg;
		}
	
	public void buildButton( float x, float y, GameContainer gc ) throws SlickException {
		rect = new Rectangle( x, y, width, height );
	}
	
	public int getIndex()
		{ return index; }
	
	public boolean isClickable()
		{ return clickable; }
	
	/** modifica il colore del bottone*/
	public void setColor( Color color )
		{
			c = color;
			if(c == Color.gray)
				clickable = false;
			else
				clickable = true;
		}
	
	/** modifica la lunghezza del bottone*/
	public float getLungh()
		{ return rect.getWidth(); }
	
	/** modifica l'altezza del bottone*/
	public float getAlt()
		{ return rect.getHeight(); }

	/** modifica il valore di attivazione*/
	public void setActive()
		{ active = !active; }

	/** determina se il tasto e' attivo
	 * @return active - TRUE se e' attivo, FALSE altrimenti
	*/
	public boolean isActive()
		{ return active; }
	
	/** modifica il nome del livello*/
	public void setName( String name )
		{ this.name = name; }
	
	public float getMaxX()
		{ return rect.getMaxX(); }
	
	public float getMaxY()
		{ return rect.getMaxY(); }
	
	public boolean checkClick( int x, int y, Input input )
		{ return rect.contains( x, y ); }

	public void draw( Graphics g )
		{
			g.setAntiAlias( true );
			g.setColor( c );
			g.fill( rect );

			super.draw( g );

			if (pressed) {
				image.draw( rect.getX() + (width - widthImg)/2, rect.getY() + (height - heightImg)/2, widthImg, heightImg );
			} else {
				image.draw( rect.getX() + (width - widthImg)/2, rect.getY() + (height - heightImg)/2, widthImg, heightImg);
			}
		}
}