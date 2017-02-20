
package simulator;

import java.io.IOException;

import simulator.core.Simulator;
import simulator.network.NetworkLink;
import simulator.network.NetworkNode;
import simulator.network.NetworkTopology;

public class Main
{
    protected static class MyLink extends NetworkLink
    {
        public MyLink( final long sourceId, final long destId, final double bandwith, final double delay )
        {
            super( sourceId, destId, bandwith, delay );
        }
    }
    
    protected static class MyNode extends NetworkNode
    {
        public MyNode( final long id, final String name, final int xPos, final int yPos )
        {
            super( id, name, xPos, yPos );
        }
    }
    
    public static void main( String argv[] ) throws IOException
    {
        // TODO testare le varie funzionalita' del simulatore
        Simulator sim = new Simulator();
        NetworkTopology net = new NetworkTopology( "Settings/Settings.json" );
        net.addNode( new MyNode( 3L, "load_balancer", 10, 10 ) );
        net.addLink( new MyLink( 3L, 4L, 125.0d, 0.1d ) );
        System.out.println( net.toString() );
    }
}