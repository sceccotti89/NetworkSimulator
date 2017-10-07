/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animator_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalSliderUI;

import simulator.graphics.animation_swing.AnimationNetwork;

public class AnimationManager extends JPanel implements ChangeListener, ComponentListener, ActionListener
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 7463830294028112320L;
    
    private NetworkDisplay nd;
    
    private JSlider time;
    
    private List<AbstractButton> buttons;
    
    public static int frames = 1;
    
    private final String START = "START";
    private final String PAUSE = "PAUSE";
    private final String STOP  = "STOP";
    
    

    public AnimationManager( final NetworkDisplay nd, final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        //setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
        
        addComponentListener( this );
        
        this.nd = nd;
        nd.setAnimationManager( this );
        
        addTimeBar();
        
        buttons = new ArrayList<>();
        AbstractButton button = new JToggleButton( "START" );
        button.setEnabled( true );
        button.setName( START );
        button.addActionListener( this );
        buttons.add( button );
        add( button );
        button = new JToggleButton( "PAUSE" );
        button.setName( PAUSE );
        button.addActionListener( this );
        add( button );
        buttons.add( button );
        button = new JButton( "STOP" ); 
        button.setName( STOP );
        button.addActionListener( this );
        add( button );
        buttons.add( button );
    }
    
    private AbstractButton findButton( final String name )
    {
        for (AbstractButton button: buttons) {
            if (button.getName().equals( name )) {
                return button;
            }
        }
        
        return null;
    }
    
    private void addTimeBar()
    {
        // TODO per adesso mi va bene cosi', poi disegnarla da capo in modo ceh stia sopra i bottoni.
        time = new JSlider( JSlider.HORIZONTAL, 0, 200, 0 );
        time.setPreferredSize( new Dimension( getWidth() - 10, getHeight() ) );
        time.setForeground( Color.RED );
        time.setMajorTickSpacing( 1 );
        time.setMinorTickSpacing( 1 );
        time.addChangeListener( this );
        //time.setPaintTicks( true );
        //time.setPaintLabels( true );
        add( time );
        
        time.setUI( new MetalSliderUI() {
            @Override
            protected void scrollDueToClickInTrack( final int direction )
            {
                int value = slider.getValue(); 
                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    value = valueForXPosition( slider.getMousePosition().x );
                } else if (slider.getOrientation() == JSlider.VERTICAL) {
                    value = valueForYPosition( slider.getMousePosition().y );
                }
                slider.setValue( value );
            }
        } );
    }
    
    @Override
    public void stateChanged( final ChangeEvent e )
    {
        double value = time.getValue();
        long timer = (long) (AnimationNetwork.timeSimulation * (value / time.getMaximum()));
        nd.setTime( timer );
    }
    
    public void update()
    {
        if (!nd.isPauseAnimation()) {
            long timer = nd.getTime();
            time.setValue( (int) (time.getMaximum() * (timer / (double) AnimationNetwork.timeSimulation)));
        }
    }
    
    @Override
    public void componentResized( final ComponentEvent e )
    {
        remove( time );
        addTimeBar();
    }
    
    @Override
    public void componentMoved( final ComponentEvent e ) {}
    @Override
    public void componentShown( final ComponentEvent e ) {}
    @Override
    public void componentHidden( final ComponentEvent e ) {}
    
    
    public void resetButtons()
    {
        for (AbstractButton button : buttons) {
            button.setSelected( false );
        }
    }

    @Override
    public void actionPerformed( final ActionEvent e )
    {
        AbstractButton button = (AbstractButton) e.getSource();
        switch (button.getName()) {
            case( START ):
                nd.startAnimation();
                button.setSelected( true );
                findButton( PAUSE ).setSelected( false );
                break;
            case( STOP ):
                nd.stopAnimation();
                resetButtons();
                break;
            case( PAUSE ):
                nd.pauseAnimation();
                findButton( START ).setSelected( false );
                break;
        }
    }

    protected void paintComponent( final Graphics g )
    {
        super.paintComponent( g );
        
        // TODO disegnare la barra del tempo dopo aver "creato" il suo spazio.
        
    }
}