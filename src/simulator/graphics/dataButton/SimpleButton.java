
package simulator.graphics.dataButton;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;
import org.newdawn.slick.geom.Rectangle;

public class SimpleButton extends Button
{
    /* determina se e' possibile premere il tasto */
    private boolean active = true;
    /* il colore del bottone */
    private Color c;
    
    private static UnicodeFont font;
    private float ratioH = 1.f;
    // il rapproto di grandezza del font
    private float ratioFont = 10.0f;
    // determina se il bottone e' cliccabile
    private boolean clickable;
    // l'indice del bottone
    private int index;
    /* lunghezza e altezza del bottone triangolare */
    float width, height;

    /** crea un nuovo bottone rettangolare
     * @param x - coordinata X
     * @param y - coordinata Y
     * @param name - il nome del bottone
     * @throws SlickException 
    */
    public SimpleButton( float x, float y, float width, float height, String name, Color color, int index, GameContainer gc ) throws SlickException
        {
            super();
            
            c = color;

            this.name = name;
            this.width = width;
            this.height = height;
            buildButton( x, y, gc );
            
            clickable = true;
            
            this.index = index;
        }
    
    @SuppressWarnings("unchecked")
    public void buildButton( float x, float y, GameContainer gc ) throws SlickException
        {    
            ratioFont = ratioFont * ratioH;
            
            font = new UnicodeFont( "./data/fonts/prstart.ttf", (int)(ratioFont), false, true );
            font.addAsciiGlyphs();
            font.addGlyphs( gc.getWidth(), gc.getHeight() );
            font.getEffects().add( new ColorEffect( java.awt.Color.WHITE ) );
            font.loadGlyphs();

            rect = new Rectangle( x, y, width, height );
        }
    
    public int getIndex()
        { return index; }
    
    public boolean isClickable()
        { return clickable; }
    
    /** modifica*/
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
        {  return rect.getWidth(); }
    
    /** modifica l'altezza del bottone*/
    public float getAlt()
        {  return rect.getHeight(); }

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

    public void draw( Graphics g )
        {
            g.setAntiAlias( true );
            g.setColor( c );
            g.fill( rect );

            super.draw( g );

            float width = 1.f;
            if (pressed) {                
                font.drawString( rect.getX() + this.width/2 - font.getWidth( name )/2 + width, rect.getY() + this.height/2 - font.getHeight( name )/2 + width, name, Color.black );                
            } else {                
                font.drawString( rect.getX() + this.width/2 - font.getWidth( name )/2, rect.getY() + this.height/2 - font.getHeight( name )/2, name, Color.black );
            }
            
            if(!active)
                {
                    g.setColor( new Color( 0, 0, 0, 100 ) );
                    g.fill( rect );
                }
        }
}
