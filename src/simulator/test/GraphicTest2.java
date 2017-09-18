
package simulator.test;
 
 import java.io.IOException;

import simulator.graphics.animator_swing.AnimationNetwork;
 
 public class GraphicTest2
 {
     public static void main( final String[] args ) throws IOException
     {
         //Plotter plotter = new Plotter( "prova", 800, 600 );
         //plotter.addPlot( "C:\\Users\\Stefano\\Desktop\\5.txt", null, "5.txt" );
         //plotter.setVisible( true );
         
         AnimationNetwork an = new AnimationNetwork( 800, 600, "prova" );
         an.setTimer( 90 );
         
         //an.loadNetwork( "Topology/Topology_distributed_multiNode.json" );
         an.loadSimulation( "Topology/Topology_distributed_singleNode.json", "data/File/Packets.txt" );
         //an.loadSimulation( "Topology/Topology_multicore.json", "Results/packets.txt" );
        
         //an.addNode( 100, 200, 3, Color.red );
         //an.addLink(  );
         
         an.start();
     }
 } 