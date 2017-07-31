/**
 * @author Stefano Ceccotti
*/

package simulator.graphics_swing.interfaces;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class AnimationManager extends JPanel
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = -5413597974128852252L;
    
    private List<JToggleButton> buttons;

    public AnimationManager( final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        setLayout( new GridLayout( 1, 4 ) );
        
        buttons = new ArrayList<>();
        JToggleButton button = new JToggleButton( "START" );
        button.setEnabled( true );
        buttons.add( button );
        add( button );
        button = new JToggleButton( "PAUSE" );
        add( button );
        buttons.add( button );
        button = new JToggleButton( "STOP" );
        add( button );
        buttons.add( button );
        
        // TODO aggiungere un'area per la gestione del tempo di aggiornamento dell'animazione
        
    }
    
    public void setNetworkDisplay( final NetworkDisplay nd ) {
        // START
        buttons.get( 0 ).addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                nd.startAnimation();
            }
        } );
        
        // PAUSE
        buttons.get( 1 ).addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                nd.pauseAnimation();
            }
        } );
        
        // STOP
        buttons.get( 2 ).addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                nd.stopAnimation();
            }
        } );
    }
    
    public void resetButtons()
    {
        for (JToggleButton button : buttons) {
            button.setSelected( false );
        }
    }
}