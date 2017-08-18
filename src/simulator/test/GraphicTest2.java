
package simulator.test;
 
 import java.io.IOException;

import simulator.graphics.plotter.Plotter;
import simulator.graphics_swing.AnimationNetwork;
 
 public class GraphicTest2
 {
     public static void main( final String[] args ) throws IOException
     {
         /*Evaluator evaluator = new Evaluator( "log10(4) + asin(0.1)^2 * cos(0.1)^3" ); // 0.61194
         evaluator.putVariable( "x", 0.1 );
         evaluator.putVariable( "y", 0.1 );
         System.out.println( evaluator.eval() );
         System.exit( 0 );*/
         
         Plotter plotter = new Plotter( "prova", 800, 600 );
         plotter.addPlot( "/home/stefano/test.txt", null, "test.txt" );
         plotter.setVisible( true );
         
         AnimationNetwork an = new AnimationNetwork( 800, 600, "prova" );
         an.setTimer( 90 );
         
         an.loadSimulation( "Topology/Topology_distributed_singleNode.json", "data/File/Packets.txt" );
         //an.loadSimulation( "Topology/Topology_multicore.json", "Results/packets.txt" );
        
         //an.addNode( 100, 200, 3, Color.red );
         //an.addLink(  );
         
         an.start();
     }
 } 