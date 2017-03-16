
package simulator.graphics;

import java.awt.Color;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

import simulator.graphics.dataButton.SimpleButton;
import simulator.graphics.interfaces.AnimationManager;
import simulator.graphics.interfaces.OptionBar;
import simulator.graphics.interfaces.TimeAnimation;

public class AnimationNetwork extends BasicGame
{
    private OptionBar ob;
    private AnimationManager am;
    private TimeAnimation ta;
    
    public AnimationNetwork( final String title )
    {
        super( title );
    }

    @Override
    public void init( final GameContainer gc ) throws SlickException
    {
        ob = new OptionBar();
        am = new AnimationManager( gc, ob.getMaxY() );
        ta = new TimeAnimation();
    }

    @Override
    public void update( final GameContainer gc, final int delta ) throws SlickException
    {
    }

    @Override
    public void render( final GameContainer gc, final Graphics g ) throws SlickException
    {
        ob.render( g );
        am.render( g );
        ta.render( g );
    }

}