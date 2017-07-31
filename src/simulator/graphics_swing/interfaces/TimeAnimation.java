
package simulator.graphics_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TimeAnimation extends JPanel implements ChangeListener
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 7463830294028112320L;
    
    private NetworkDisplay nd;
    
    private long maxTimer;
    private long timer = 0;
    private JSlider time;

    public TimeAnimation( final NetworkDisplay nd, final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        
        this.nd = nd;
        
        time = new JSlider( JSlider.HORIZONTAL, 0, 10, 0 );
        time.setPreferredSize( new Dimension( (int) width - 10, (int) height ) );
        time.setForeground( Color.RED );
        //time.setMajorTickSpacing( 10 );
        //time.setMinorTickSpacing( 1 );
        time.addChangeListener( this );
        //time.setPaintTicks( true );
        //time.setPaintLabels( true );
        add( time );
    }

    @Override
    public void stateChanged( final ChangeEvent e )
    {
        long value = time.getValue();
        timer = maxTimer * value;
        nd.setTime( timer );
    }
    
    public void update( final int delta )
    {
        timer = timer + delta;
        time.setValue( (int) (maxTimer/timer) );
    }
    
    // TODO controllare se sia il caso di fare override del metodo paintComponents.
}