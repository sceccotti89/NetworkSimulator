
package simulator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.core.Event;
import simulator.core.EventGenerator;
import simulator.core.Simulator;
import simulator.core.Time;
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
    
    protected static class RandomGenerator extends EventGenerator
    {
        public RandomGenerator( final Time duration, final Time arrivalTime, final Time serviceTime )
        {
            super( duration, arrivalTime, serviceTime );
        }
        
        @Override
        public void generate( final NetworkNode destNode )
        {
            Event next = nextEvent();
        }

        @Override
        public Event nextEvent()
        {
            if(_time.compareTo( _duration ) > 0)
                return null;
            // TODO generare un nuovo evento, settando pero' il tempo come quello attuale.
            Event next = null;
            
            // TODO aggiornare il tempo attuale.
            
            return next;
        }
    }
    
    public static void main( String argv[] ) throws IOException
    {
        Simulator sim = new Simulator();
        NetworkTopology net = new NetworkTopology( "Settings/Settings.json" );
        net.addNode( new MyNode( 3L, "load_balancer", 10, 10 ) );
        net.addLink( new MyLink( 3L, 0L, 125.0d, 0.1d ) );
        System.out.println( net.toString() );
        
        sim.addNetworkTopology( net );
        
        RandomGenerator generator = new RandomGenerator( new Time(480, TimeUnit.MINUTES),
                                                         new Time(10,  TimeUnit.MINUTES),
                                                         new Time(5,   TimeUnit.MINUTES) );
        //sim.addGenerator( generator );
        
        sim.start();
        
        sim.stop();
    }
}