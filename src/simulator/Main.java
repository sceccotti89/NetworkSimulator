
package simulator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.Agent.ActiveAgent;
import simulator.coordinator.Event;
import simulator.coordinator.Event.RequestEvent;
import simulator.coordinator.EventGenerator;
import simulator.coordinator.EventHandler;
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
        public void generate( final EventHandler evHandler, final NetworkNode destNode )
        {
            Event next = nextEvent( evHandler );
            // TODO essere sicuri che il tempo sia quello giusto.
            evHandler.schedule( next );
        }
        
        // TODO non sono sicuro debba essere overraidata
        // TODO ogni generatore fa questa cosa..
        @Override
        public Event nextEvent( final EventHandler evHandler )
        {
            Time time = evHandler.getTime();
            if(time.compareTo( _duration ) > 0)
                return null; // Stop the generator.
            
            Event next = new RequestEvent( time );
            return next;
        }
    }
    
    protected static class MyAgent extends ActiveAgent
    {
        public MyAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
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
        
        MyAgent agent = new MyAgent( 0, generator );
        sim.addAgent( agent );
        
        // TODO aggiungere i vari nodi (o tramite lista)
        // TODO ognuno deve essere assegnato a un topologyNode => lo usero' per il calcolo del percorso piu' breve alla destinazione
        
        // TODO aggiungere l'opzione di poter lanciare la simulazione per tot secondi
        sim.start();
        
        sim.stop();
    }
}