
package simulator.test;
 
import java.io.IOException;

import org.newdawn.slick.SlickException;

import simulator.graphics.AnimationNetwork;
 
public class GraphicTest
{
    public static void main( String[] args ) throws SlickException, IOException
    {
        AnimationNetwork an = new AnimationNetwork( 800, 600, "prova" );
        
        //an.loadSimulation( "Topology/Topology_multicore.json", "data/File/Packets.txt" );
        //an.loadNetwork( "Topology/Topology_distributed_multiCore.json" );
        //an.loadSimulation( "Topology/Topology_distributed_multiCore.json", "Results/packets.txt" );
        
        an.loadSimulation( "Topology/Animation/Topology_distributed_multiCore.json", "Results/distr_multi_core.txt" );
        
        //an.addNode( 100, 200, 3, Color.red );
        //an.addLink(  );
        
        an.setTargetFrameRate( 90 );
        an.start();
    }
}
