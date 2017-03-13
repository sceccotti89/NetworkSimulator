
package simulator.graphics.interfaces;

import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class OptionBar implements AnimationInterface
{
    private Rectangle barOptions = new Rectangle( 0, 0, 800, 20 );
    
    public float getMaxY() {
        return barOptions.getMaxY();
    }
    
    @Override
    public void update( final int delta )
    {
        
    }
    
    @Override
    public void render( final Graphics g )
    {
        g.draw( barOptions );
    }
}