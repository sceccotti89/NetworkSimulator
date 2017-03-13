
package simulator.graphics.interfaces;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class TimeAnimation implements AnimationInterface
{
    private Rectangle barTiming  = new Rectangle( 0, 520, 800, 80 );
    
    @Override
    public void update( final int delta )
    {
        
    }
    
    @Override
    public void render( final Graphics g )
    {
        g.draw( barTiming );
    }
}