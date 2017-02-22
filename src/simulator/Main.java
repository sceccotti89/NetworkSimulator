
package simulator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.Agent.ActiveAgent;
import simulator.Agent.PassiveAgent;
import simulator.coordinator.EventGenerator;
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
    
    protected static class CBRGenerator extends EventGenerator
    {
        public CBRGenerator( final Time duration,
                             final Time departureTime,
                             final Time serviceTime )
        {
            super( duration, departureTime, serviceTime );
        }
        
        public CBRGenerator( final Time duration,
                             final Time departureTime,
                             final Time serviceTime,
                             final Agent from,
                             final Agent to )
        {
            super( duration, departureTime, serviceTime, from, to );
        }
        
        /*@Override
        public void generate( final EventHandler evHandler, final NetworkNode destNode )
        {
            Event next = nextEvent();
            evHandler.schedule( next );
        }*/
    }
    
    protected static class ClientAgent extends ActiveAgent
    {
        public ClientAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
    }
    
    protected static class ServerAgent extends PassiveAgent
    {
        public ServerAgent( final long id )
        {
            super( id );
        }
    }
    
    public static void main( final String argv[] ) throws IOException
    {
        Simulator sim = new Simulator();
        NetworkTopology net = new NetworkTopology( "Settings/Topology.json" );
        net.addNode( new MyNode( 3L, "load_balancer", 10, 10 ) );
        net.addLink( new MyLink( 3L, 0L, 125.0d, 0.1d ) );
        System.out.println( net.toString() );
        
        sim.setNetworkTopology( net );
        
        CBRGenerator generator = new CBRGenerator( new Time(11, TimeUnit.MINUTES),
                                                   new Time(10, TimeUnit.MINUTES),
                                                   new Time(5,  TimeUnit.MINUTES) );// TODO non sono sicuro che l'ultimo parametro lo voglia settare in questa
                                                                                    // TODO maniera. Potrei usare la stessa tecnica di NS2
        Agent client = new ClientAgent( 0, generator );
        sim.addAgent( client );
        
        Agent server = new ServerAgent( 1 );
        sim.addAgent( server );
        
        generator.setLink( client, server ).setWaitReponse( false );
        
        // TODO aggiungere l'opzione di poter lanciare la simulazione per tot secondi
        sim.start();
        
        sim.stop();
    }
}