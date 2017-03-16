
package simulator.graphics.interfaces;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.SimpleButton;

public class AnimationManager implements AnimationInterface
{
    private Rectangle play, stop, pause, fastSlow;
    private float width, height;
    
    private SimpleButton start, fermo, pausa, speed;
    
    public AnimationManager( final GameContainer gc, final float startY ) throws SlickException
    {
        height = gc.getHeight()*10/75;
        width = gc.getWidth()*10/53;
        
        play     = new Rectangle( 0, startY, width, height );
        stop     = new Rectangle( play.getMaxX(), startY, width, height );
        pause    = new Rectangle( stop.getMaxX(), startY, width, height );
        fastSlow = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        
        // TODO COMPLETARE I VARI BOTTONI        
        start = new SimpleButton( 0, startY, width, height, "START", Color.gray, 0, gc );
        fermo = new SimpleButton( start.getMaxX(), startY, width, height, "STOP", Color.gray, 1, gc );
        pausa = new SimpleButton( fermo.getMaxX(), startY, width, height, "PAUSE", Color.gray, 2, gc );
        speed = new SimpleButton( pausa.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height, "SPEED", Color.gray, 3, gc );
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
        
        start.draw( g );
        fermo.draw( g );
        pausa.draw( g );
        speed.draw( g );
    }
}