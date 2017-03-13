
package simulator.test;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.SlickException;

import simulator.graphics.AnimationNetwork;

public class GraphicTest
{
    public static void main( final String[] args ) throws SlickException
    {
        AnimationNetwork an = new AnimationNetwork( "prova" );
        AppGameContainer app = new AppGameContainer( an );
        
        app.setTargetFrameRate( 90 );
        app.setDisplayMode( 800, 600, false );
        
        app.start();
    }
}