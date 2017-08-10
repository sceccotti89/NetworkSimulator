
package simulator.test;
 
 import java.io.IOException;

import simulator.graphics.plotter.evaluator.Evaluator;
import simulator.graphics_swing.AnimationNetwork;
 
 public class GraphicTest2
 {
     public static void main( final String[] args ) throws IOException
     {
         //Evaluator evaluator = new Evaluator( "sqrt(2^10) + 5*asin(y) - cos(x)^3" );
         // FIXME manca la precedenza degli operatori.
         // FIXME manca la corretta gestione dei token con le funzioni log.
         Evaluator evaluator = new Evaluator( "3 + 5*4 - 2*2" );
         evaluator.putVariable( "x", 0.1 );
         evaluator.putVariable( "y", 0.3 );
         System.out.println( evaluator.eval() );
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