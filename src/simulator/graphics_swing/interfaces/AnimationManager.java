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

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class AnimationManager extends JPanel
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = -5413597974128852252L;
    
    private List<AbstractButton> buttons;
    
    public static int frames = 1;
    
    public final String START = "START", PAUSE = "PAUSE", STOP = "STOP";

    public AnimationManager( final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        setLayout( new GridLayout( 1, 4 ) );
        
        buttons = new ArrayList<>();
        AbstractButton button = new JToggleButton( "START" );
        button.setEnabled( true );
        button.setName( START );
        buttons.add( button );
        add( button );
        button = new JToggleButton( "PAUSE" );
        button.setName( PAUSE );
        add( button );
        buttons.add( button );
        button = new JButton( "STOP" );
        button.setName( STOP );
        add( button );
        buttons.add( button );
        
        // TODO aggiungere un'area per la gestione del tempo di aggiornamento dell'animazione
        
    }
    
    public void setNetworkDisplay( final NetworkDisplay nd )
    {
    	for (AbstractButton button: buttons) {
    		if (button.getName().equals( START )) {
    			button.addActionListener( new ActionListener() {
    	            @Override
    	            public void actionPerformed( final ActionEvent e ) {
    	                nd.startAnimation();
    	                buttons.get( 1 ).setSelected( false );
    	            }
    	        } );
    		} else if (button.getName().equals( PAUSE )) {
    			button.addActionListener( new ActionListener() {
    	            @Override
    	            public void actionPerformed( final ActionEvent e ) {
    	            	nd.pauseAnimation();
    	                buttons.get( 0 ).setSelected( false );
    	            }
    	        } );
    		} else if (button.getName().equals( STOP )) {
    			button.addActionListener( new ActionListener() {
    	            @Override
    	            public void actionPerformed( final ActionEvent e ) {
    	            	nd.stopAnimation();
    	                resetButtons();
    	            }
    	        } );
    		}
    	}
    	
        // START
        /*buttons.get( 0 ).addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                nd.startAnimation();
                buttons.get( 1 ).setSelected( false );
            }
        } );
        
        // PAUSE
        buttons.get( 1 ).addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                nd.pauseAnimation();
                buttons.get( 0 ).setSelected( false );
            }
        } );
        
        // STOP
        buttons.get( 2 ).addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                nd.stopAnimation();
                resetButtons();
            }
        } );*/
    }
    
    public void resetButtons()
    {
        for (AbstractButton button : buttons) {
            button.setSelected( false );
        }
    }
}