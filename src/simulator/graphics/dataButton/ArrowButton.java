
package simulator.graphics.dataButton;

import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Polygon;

public class ArrowButton
{
    /* l'area della freccia*/
    private Polygon row;
    /* colore del bottone*/
    private Color c;
    /* determina se e' stata premuta*/
    private boolean pressed = false;
    /* la direzione della freccia*/
    private int direction;
    /* tipi di direzione della feccia*/
    public static final int RIGHT = 0, LEFT = 1;
    /* i punti della freccia */
    private float[] points;
    /* il nome del bottone */
    public String name;
    /* l'indice della freccia */
    public int index;

    public ArrowButton( String name, int direction, float points[], Color color, int index )
    {
        this.points = points;
        c = color;
        this.direction = direction;
        row = new Polygon( points );
        this.name = name;
        this.index = index;
    }

    /** modifica lo stato (premuto/non premuto) della freccia*/
    public void setPressed( final boolean val )
    {
        pressed = val;
    }
    
    /** restituisce l'indice della freccia */
    public int getIndex()
    {
        return index;
    }
    
    /** restituisce il nome del bottones
     * @return name - il nome del bottone
    */
    public String getName()
    {
        return name;
    }

    /** restituisce il valore di pressione sul bottone
     * @return pressed - TRUE se e' stato premuto, FALSE altrimenti
    */
    public boolean isPressed()
    {
        return pressed;
    }

    /** restituisce la coordinata X dell'area
     * @return x - la coordinata X
    */
    public float getX()
    {
        return row.getX();
    }

    /** restituisce la coordinata Y dell'area
     * @return y - la coordinata Y
    */
    public float getY()
    {
        return row.getY();
    }

    /** restituisce il punto piu a sinistra dell'area
     * @return maxX - la coordinata X piu' a sinistra
    */
    public float getMaxX()
    {
        return row.getMaxX();
    }

    /** restituisce l'altezza dell'area
     * @return height - l'altezza dell'area
    */
    public float getHeight()
    {
        return row.getHeight();
    }
    
    /** restituisce la lunghezza dell'area
     * @return height - l'altezza dell'area
    */
    public float getWidth()
    {
        return row.getWidth();
    }
    
    /** restituisce la direzione della freccia
     * @return height - l'altezza dell'area
    */
    public float getDirection()
    {
        return direction;
    }

    /** sposta la coordinate X e Y
     * @param value - il valore di spostamento
    */
    public void translate( float valueX, float valueY )
    {
        for(int i = 0; i < points.length; i++)
            if(i % 2 == 0)
                points[ i ] = points[ i ] * valueX;
            else
                points[ i ] = points[ i ] * valueY;
        
        row = new Polygon( points );
    }

    /** determina se le coordinate del mouse sono contenute nell'area
     * @param x - la coordinata X del mouse
     * @param y - la coordinata Y del mouse
     * 
     * @return TRUE se sono contenute, FALSE altrimenti
    */
    public boolean contains( int x, int y )
    {
        return row.contains( x, y );
    }

    /** disegna la feccia
     * @param g - il contesto grafico
    */
    public void draw( Graphics g )
    {
        g.setColor( c );
        g.fill( row );

        // aggiunge l'ombreggiatura
        g.setLineWidth( 1.f );
        float x = row.getX(), y = row.getY(), w = row.getWidth(), h = row.getHeight();

        if(direction == LEFT){
            // laterale
            g.setColor( pressed ? Color.lightGray : Color.black );
            g.drawLine( x + w, y, x + w, y + h - 1 );
            g.setColor( pressed ? Color.lightGray : Color.darkGray );
            g.drawLine( x + w + 1, y - 2, x + w + 1, y + h );

            // in alto
            g.setColor( pressed? Color.darkGray : Color.lightGray );
            g.drawLine( x - 1, y + h/2, x + w + 1, y - 1 );
            g.setColor( Color.lightGray );
            g.drawLine( x - 2, y + h/2, x + w + 1, y - 2 );

            // in basso
            g.setColor( pressed? Color.black : Color.darkGray );
            g.drawLine( x - 1, y + h/2, x + w + 1, y + h );
            g.setColor( pressed? Color.darkGray : Color.lightGray );
            g.drawLine( x - 2, y + h/2, x + w + 1, y + h + 1 );
        }
        else{
            // laterale
            g.setColor( !pressed ? Color.lightGray : Color.black );
            g.drawLine( x - 1, y - 1, x - 1, y + h - 1 );
            g.setColor( !pressed ? Color.lightGray : Color.darkGray );
            g.drawLine( x - 2, y - 1, x - 2, y + h );

            // in alto
            g.setColor( !pressed? Color.darkGray : Color.lightGray );
            g.drawLine( x, y, x + w + 1, y + h/2 );
            g.setColor( Color.lightGray );
            g.drawLine( x, y - 1, x + w + 2, y + h/2 );

            // in basso
            g.setColor( !pressed? Color.black : Color.darkGray );
            g.drawLine( x - 1, y + h, x + w + 1, y + h/2 );
            g.setColor( !pressed? Color.darkGray : Color.lightGray );
            g.drawLine( x - 1, y + h + 1, x + w + 2, y + h/2 );
        }
    }
}
