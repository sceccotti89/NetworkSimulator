
package simulator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.Agent.ActiveAgent;
import simulator.Agent.PassiveAgent;
import simulator.coordinator.EventGenerator;
import simulator.coordinator.EventGenerator.ConstantEventGenerator;
import simulator.core.Simulator;
import simulator.core.Time;
import simulator.network.NetworkTopology;
import simulator.utils.SimulatorUtils;

public class Main
{
    protected static class CBRGenerator extends ConstantEventGenerator
    {
        public CBRGenerator( final Time duration,
                             final Time departureTime,
                             final Packet pkt )
        {
            super( duration, departureTime, pkt );
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
    	example1();
    }
    
    private static void example1() throws IOException
    {
    	NetworkTopology net = new NetworkTopology( "Settings/Topology_ex1.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 10, TimeUnit.MINUTES ),
                                                   new Time( 5,  TimeUnit.MINUTES ),
                                                   new Packet( 40, SimulatorUtils.Size.KB ) );
        Agent client = new ClientAgent( 0, generator );
        sim.addAgent( client );
        
        /*CBRGenerator generator2 = new CBRGenerator( new Time( 5, TimeUnit.MINUTES ),
                                                    new Time( 1, TimeUnit.MINUTES ),
                                                    new Packet( 20, Packet.Size.B ) );
        Agent client2 = new ClientAgent( 1, generator2 );
        sim.addAgent( client2 );*/
        
        Agent server = new ServerAgent( 2 );
        sim.addAgent( server );
        
        client.connect( server );
        //client2.connect( server );
        
        sim.start();
        
        sim.stop();
    }
}