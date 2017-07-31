/**
 * @author Stefano Ceccotti
*/

package simulator.graphics_swing.interfaces;

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;

public class AnimationManager extends JPanel
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = -5413597974128852252L;

    public AnimationManager( final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        
        JButton button1 = new JButton( "START" );
        add( button1 );
        JButton button2 = new JButton( "PAUSE" );
        add( button2 );
        JButton button3 = new JButton( "STOP" );
        add( button3 );
        
        // TODO aggiungere un'area per la gestione del tempo di aggiornamento dell'animazione
        
    }
}