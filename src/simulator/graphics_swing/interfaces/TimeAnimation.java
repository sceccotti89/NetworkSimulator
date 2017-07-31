
package simulator.graphics_swing.interfaces;

import java.awt.Dimension;

import javax.swing.JPanel;

public class TimeAnimation extends JPanel
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 7463830294028112320L;

    public TimeAnimation( final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
    }
    
    // TODO controllare se sia il caso di fare override dl metodo paintComponents.
}