
package simulator.graphics_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalSliderUI;

import simulator.graphics_swing.AnimationNetwork;

public class TimeAnimation extends JPanel implements ChangeListener
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 7463830294028112320L;
    
    private NetworkDisplay nd;
    
    private JSlider time;

    public TimeAnimation( final NetworkDisplay nd, final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        
        this.nd = nd;
        
        time = new JSlider( JSlider.HORIZONTAL, 0, 200, 0 );
        time.setPreferredSize( new Dimension( (int) width - 10, (int) height ) );
        time.setForeground( Color.RED );
        time.setMajorTickSpacing( 1 );
        time.setMinorTickSpacing( 1 );
        time.addChangeListener( this );
        time.setPaintTicks( true );
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
            time.setValue( (int) (time.getMaximum() * (timer / (double) AnimationNetwork.timeSimulation)) );
        }
    }
    
    // TODO controllare se sia il caso di fare override del metodo paintComponents. Probabilmente si.
}