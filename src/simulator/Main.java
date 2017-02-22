
package simulator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.Agent.ActiveAgent;
import simulator.Agent.PassiveAgent;
import simulator.coordinator.EventGenerator;
import simulator.coordinator.EventGenerator.ConstantEventGenerator;
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
    
    protected static class CBRGenerator extends ConstantEventGenerator
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
        NetworkTopology net = new NetworkTopology( "Settings/Topology.json" );
        net.addNode( new MyNode( 2L, "load_balancer", 10, 10 ) );
        net.addLink( new MyLink( 2L, 0L, 125.0d, 0.1d ) );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time(11, TimeUnit.MINUTES),
                                                   new Time(10, TimeUnit.MINUTES),
                                                   new Time(5,  TimeUnit.MINUTES) );// TODO non sono sicuro che l'ultimo parametro lo voglia settare in questa
                                                                                    // TODO maniera. Potrei usare la stessa tecnica di NS2.
        Agent client = new ClientAgent( 0, generator );
        sim.addAgent( client );
        
        CBRGenerator generator2 = new CBRGenerator( new Time(5, TimeUnit.MINUTES),
                                                    new Time(1, TimeUnit.MINUTES),
                                                    new Time(1, TimeUnit.MINUTES) );// TODO non sono sicuro che l'ultimo parametro lo voglia settare in questa
                                                                                    // TODO maniera. Potrei usare la stessa tecnica di NS2.
        Agent client2 = new ClientAgent( 1, generator2 );
        sim.addAgent( client2 );
        
        Agent server = new ServerAgent( 2 );
        sim.addAgent( server );
        
        // TODO questi 2 comandi non servirebbe se l'informazione la prendesse in automatico quando lancia un evento,
        // TODO basterebbe accedere il networkTopology
        generator.setLink( client, server );
        generator2.setLink( client2, server );
        
        sim.start();
        
        sim.stop();
    }
}