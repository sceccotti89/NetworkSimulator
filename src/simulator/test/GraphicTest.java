package simulator.test;
 
 import java.io.IOException;

import org.newdawn.slick.SlickException;

import simulator.graphics.AnimationNetwork;
 
 public class GraphicTest
 {
     public static void main( final String[] args ) throws SlickException, IOException
     {
         AnimationNetwork an = new AnimationNetwork( 800, 600, "prova" );
         
         an.loadSimulation( "Topology/Topology_multicore.json", "data/File/Packets.txt" );
         //an.loadSimulation( "Topology/Topology_multicore.json", "Results/packets.txt" );
        
         //an.addNode( 100, 200, 3, Color.red );
         //an.addLink(  );
         
         an.setTargetFrameRate( 90 );
         an.start();
     }
 } 