
package simulator.graphics_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class NetworkDisplay extends JPanel
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 6828820513635876566L;

    public NetworkDisplay( final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
    }
    
    @Override
    protected void paintComponent( final Graphics g )
    {
        super.paintComponent( g );
        
        // TODO animare l'interfaccia.
        setBackground( Color.LIGHT_GRAY );
    }
}