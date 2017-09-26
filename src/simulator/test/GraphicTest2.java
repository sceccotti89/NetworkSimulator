
package simulator.test;
 
import java.io.IOException;

import simulator.graphics.plotter.Plotter;
 
public class GraphicTest2
{
    public static void main( final String[] args ) throws IOException
    {
        Plotter plotter = new Plotter( "prova", 800, 600 );
        plotter.addPlot( "C:\\Users\\Stefano\\Desktop\\test.txt", null, "test.txt" );
        plotter.setVisible( true );
        
        /*AnimationNetwork an = new AnimationNetwork( 800, 600, "prova" );
        an.setTimer( 90 );
        an.loadSimulation( "Topology/Animation/Topology_distributed_multiCore.json", "data/File/Packets.txt" );
         
        an.start();*/
    }
}