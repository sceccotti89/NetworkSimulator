
package simulator.test;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.SlickException;

import simulator.graphics.AnimationNetwork;

public class GraphicTest
{
    public static void main( final String[] args ) throws SlickException
    {
        AnimationNetwork an = new AnimationNetwork( 800, 600, "prova" );
        AppGameContainer app = new AppGameContainer( an );
        
        an.loadSimulation( "data/File/Network.xml" );
        
        //an.addNode( 100, 200, 3, Color.red );
        //an.addLink(  );
        
        app.setTargetFrameRate( 90 );
        app.setDisplayMode( 800, 600, false );
        
        app.start();
    }
}