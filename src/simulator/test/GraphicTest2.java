
package simulator.test;
 
 import java.io.IOException;

import simulator.graphics.plotter.parser.FunctionInterpreter;
import simulator.graphics.plotter.parser.FunctionParser;
import simulator.graphics_swing.AnimationNetwork;
 
 public class GraphicTest2
 {
     public static void main( final String[] args ) throws IOException
     {
         FunctionParser parser = new FunctionParser( "(2^10)" );
         System.out.println( FunctionInterpreter.interpret( parser.parse(), null ) );
         System.exit( 0 );
         
         AnimationNetwork an = new AnimationNetwork( 800, 600, "prova" );
         an.setTimer( 90 );
         
         an.loadSimulation( "Topology/Topology_multicore.json", "data/File/Packets.txt" );
         //an.loadSimulation( "Topology/Topology_multicore.json", "Results/packets.txt" );
        
         //an.addNode( 100, 200, 3, Color.red );
         //an.addLink(  );
         
         an.start();
     }
 } 