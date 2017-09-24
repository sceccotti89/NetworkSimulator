/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

// TODO trasformarla in una classe che disegna soltanto la prima striscia e controlla se ha premuto il poligono:
// TODO dall'esterno controllo e agisco di conseguenza
// TODO oppure sposto tutto qui e fanculo

public class PlotsPanel
{
    private String text;
    private Font font;
    private boolean selected;
    private BufferedImage open, closed;
    public Rectangle target;
    private final int OFFSET = 30, PAD = 5;
    
    private Dimension size;
    private Color background;
    
    public PlotsPanel( final int x, final int y, final int width, final int height )
    {
        text = "Plots";
        font = new Font( "sans-serif", Font.PLAIN, 12 );
        selected = false;
        background = new Color( 200, 200, 220 );
        size = new Dimension( width, 20 );
        createImages();
    }
    
    public void mouseClicked( final MouseEvent e )
    {
        if (target.contains( e.getPoint() )) {
            selected = !selected;
        }
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    private void createImages()
    {
        int w = size.height;
        int h = size.height;
        target = new Rectangle( 2, 0, 20, 18 );
        open = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
        Graphics2D g2 = open.createGraphics();
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g2.setPaint( background );
        g2.fillRect( 0, 0, w, h );
        // TODO ragionare sulle posisioni dei poligoni
        int[] x = { 2, w / 2, 18 };
        int[] y = { 4, 15, 4 };
        Polygon p = new Polygon( x, y, 3 );
        g2.setPaint( Color.green.brighter() );
        g2.fill( p );
        g2.setPaint( Color.blue.brighter() );
        g2.draw( p );
        g2.dispose();
        
        closed = new BufferedImage( w, h, BufferedImage.TYPE_INT_RGB );
        g2 = closed.createGraphics();
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g2.setPaint( background );
        g2.fillRect( 0, 0, w, h );
        x = new int[]{ 3, 13, 3};
        y = new int[]{ 4, h / 2, 16 };
        p = new Polygon( x, y, 3 );
        g2.setPaint( Color.red );
        g2.fill( p );
        g2.setPaint( Color.blue.brighter() );
        g2.draw( p );
        g2.dispose();
    }
    
    public void draw( final JPanel panel, final Graphics2D g )
    {
        int h = size.height;
        if (selected) {
            g.drawImage( open, PAD, 0, panel );
        } else {
            g.drawImage( closed, PAD, 0, panel );
        }
        g.setFont( font );
        FontRenderContext frc = g.getFontRenderContext();
        LineMetrics lm = font.getLineMetrics( text, frc );
        float height = lm.getAscent() + lm.getDescent();
        float x = OFFSET;
        float y = (h + height) / 2 - lm.getDescent();
        g.drawString( text, x, y );
    }
}