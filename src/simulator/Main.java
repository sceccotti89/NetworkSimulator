
package simulator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.Agent.ActiveAgent;
import simulator.Agent.PassiveAgent;
import simulator.coordinator.EventGenerator;
import simulator.coordinator.EventGenerator.ConstantEventGenerator;
import simulator.core.Simulator;
import simulator.core.Time;
import simulator.exception.SimulatorException;
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
    
    public static void main( final String argv[] ) throws Exception
    {
    	//example1();
    	//example2();
    	example3();
    }
    
    public static void example1() throws IOException, SimulatorException
    {
        /*
               70Mb,5ms         50Mb,3ms
        client --------> switch --------> server
         10ms             7ms              5ms
        */
        
    	NetworkTopology net = new NetworkTopology( "Settings/Topology_ex1.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                   new Time( 5,  TimeUnit.SECONDS ),
                                                   new Packet( 40, SimulatorUtils.Size.KB ) );
        Agent client = new ClientAgent( 0, generator );
        sim.addAgent( client );
        
        Agent server = new ServerAgent( 2 );
        sim.addAgent( server );
        
        client.connect( server );
        
        sim.start();
        
        sim.stop();
    }
    
    public static void example2() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms         50Mb,3ms
        client1 <--------> switch <--------> client2
         10ms               7ms                5ms
        */
        
    	// Use 2 active generators, in a bi-directional graph.
    	NetworkTopology net = new NetworkTopology( "Settings/Topology_ex2.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                   new Time( 5,  TimeUnit.SECONDS ),
                                                   new Packet( 40, SimulatorUtils.Size.KB ) );
        Agent client1 = new ClientAgent( 0, generator );
        sim.addAgent( client1 );
        
        CBRGenerator generator2 = new CBRGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                    new Time( 1, TimeUnit.SECONDS ),
                                                    new Packet( 20, SimulatorUtils.Size.KB ) );
        Agent client2 = new ClientAgent( 2, generator2 );
        sim.addAgent( client2 );
        
        client1.connect( client2 );
        client2.connect( client1 );
        
        sim.start();
        
        sim.stop();
    }
    
    public static void example3() throws IOException, SimulatorException
    {
        /*
        client1 \							   / server1
          10ms	 \ 70Mb,5ms			100Mb,2ms /    5ms
                  \							 /
                   switch1 ---------- switch2
                  /  7ms    50Mb,3ms   5ms   \
                 / 70Mb,5ms			 80Mb,3ms \
        client2 /							   \ server2
         10ms									   5ms
        */
        
    	NetworkTopology net = new NetworkTopology( "Settings/Topology_ex3.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator1 = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                    new Time( 5,  TimeUnit.SECONDS ),
                                                    new Packet( 40, SimulatorUtils.Size.KB ) );
        Agent client1 = new ClientAgent( 0, generator1 );
        sim.addAgent( client1 );
        
        CBRGenerator generator2 = new CBRGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                    new Time( 1, TimeUnit.SECONDS ),
                                                    new Packet( 20, SimulatorUtils.Size.KB ) );
        Agent client2 = new ClientAgent( 1, generator2 );
        sim.addAgent( client2 );
        
        Agent server1 = new ServerAgent( 4 );
        sim.addAgent( server1 );
        
        Agent server2 = new ServerAgent( 5 );
        sim.addAgent( server2 );
        
        client1.connect( server1 );
        client2.connect( server2 );
        
        sim.start();
        
        sim.stop();
    }
}