
package simulator.graphics.interfaces;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class AnimationManager implements AnimationInterface
{
    private Rectangle play, stop, pause, fastSlow;
    private float height;
    
    public AnimationManager( final GameContainer gc, final float startY )
    {
        height = gc.getWidth()/10;
        
        play     = new Rectangle( 0, startY, 150, height );
        stop     = new Rectangle( play.getMaxX(), startY, 150, height );
        pause    = new Rectangle( stop.getMaxX(), startY, 150, height );
        fastSlow = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
    }
    
    @Override
    public void update( final int delta )
    {
        
    }
    
    @Override
    public void render( final Graphics g )
    {
        g.draw( play );
        g.draw( stop );
        g.draw( pause );
        g.draw( fastSlow );
    }
}