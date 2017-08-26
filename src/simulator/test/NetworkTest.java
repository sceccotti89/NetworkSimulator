
package simulator.test;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Simulator;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.events.generator.CBRGenerator;
import simulator.events.generator.EventGenerator;
import simulator.events.impl.RequestEvent;
import simulator.exception.SimulatorException;
import simulator.graphics.AnimationNetwork;
import simulator.network.NetworkLayer;
import simulator.network.NetworkSettings;
import simulator.network.protocols.RIP;
import simulator.topology.NetworkTopology;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class NetworkTest
{
    private static final int CPU_CORES = 4;
    
    protected static class ClientGenerator extends EventGenerator
    {
        public ClientGenerator( final Time duration,
                                final long maxPacketsInFly,
                                final Packet reqPacket,
                                final Packet resPacket )
        {
            super( duration, Time.ZERO, maxPacketsInFly, reqPacket, resPacket, true, false, true );
        }
        
        @Override
        public Time computeDepartureTime( final Event e ) {
            // Empty method.
            return null;
        }
    }
    
    protected static class MulticastGenerator extends EventGenerator
    {
        public MulticastGenerator( final Time duration,
                                   final long maxPacketsInFly,
                                   final Packet reqPacket,
                                   final Packet resPacket )
        {
            super( duration, Time.ZERO, maxPacketsInFly, reqPacket, resPacket, false, true, true );
            setMulticast( true, false );
        }
        
        @Override
        public Time computeDepartureTime( final Event e ) {
            // Empty method.
            return null;
        }
    }
    
    private static class SwitchAgent extends Agent
    {
        public SwitchAgent( final long id, final EventGenerator evGenerator ) {
            super( id, evGenerator );
        }
        
        @Override
        public double getNodeUtilization( final Time time )
        {
            double utilization = 0;
            for (Agent agent : _destinations) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
        }
    }
    
    protected static class SinkGenerator extends simulator.events.generator.SinkGenerator
    {
        public SinkGenerator( final Time duration,
                              final Packet reqPacket,
                              final Packet resPacket )
        {
            super( duration, 1L, reqPacket, resPacket );
        }
        
        @Override
        public Packet makePacket( final Event e )
        {
            if (e instanceof RequestEvent) {
                return new Packet( 20, SizeUnit.KILOBYTE );
            } else {
                return new Packet( 40, SizeUnit.KILOBYTE );
            }
        }
    }
    
    protected static class ClientTestGenerator extends EventGenerator
    {
        private static final String QUERY_TRACE = "Settings/QueryArrivalTrace.txt";
        
        private BufferedReader queryReader;
        
        public ClientTestGenerator( final Packet reqPacket, final Packet resPacket ) throws FileNotFoundException
        {
            super( Time.INFINITE, Time.DYNAMIC, Utils.INFINITE,
                   reqPacket, resPacket, true, false, false );
            
            // Open the associated file.
            queryReader = new BufferedReader( new FileReader( QUERY_TRACE ) );
        }
        
        @Override
        public Packet makePacket( final Event e ) {
            return null;
        }
        
        @Override
        public Time computeDepartureTime( final Event e )
        {
            try {
                String queryLine = null;
                if ((queryLine = queryReader.readLine()) != null) {
                    System.out.println( "QUERY: " + Long.parseLong( queryLine ) );
                    return new Time( Long.parseLong( queryLine ), TimeUnit.MILLISECONDS );
                } else {
                    // No more lines to read.
                    queryReader.close();
                }
            } catch ( IOException e1 ) {
                System.err.println( e1 );
            }
            
            return null;
        }
    }
    
    protected static class ClientAgent extends Agent
    {
        public ClientAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
    }
    
    protected static class ServerAgent extends Agent
    {
        public ServerAgent( final long id )
        {
            super( id );
        }
    }
    
    protected static class ResponseServerAgent extends Agent
    {
        public ResponseServerAgent( final long id, final EventGenerator generator )
        {
            super( id, generator );
        }
    }
    
    protected static class ServerTestAgent extends Agent
    {
        public ServerTestAgent( final long id, final EventGenerator generator )
        {
            super( id, generator );
        }
    }
    
    public static void main( final String argv[] ) throws Exception
    {
        //example0();
    	//example1();
    	//example2();
    	//example3();
        //example4();
        //example5();
        //example6();
        //example7();
        testNetworkAnimation();
    	
    	System.out.println( "End of simulation." );
    }
    
    public static void example0() throws IOException, SimulatorException
    {
        /*
               70Mb,5ms
        client --------> server
         10ms             5ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex0.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 20, TimeUnit.SECONDS ),
                                                   new Time( 5,  TimeUnit.SECONDS ),
                                                   new Packet( 40, SizeUnit.KILOBYTE ),
                                                   new Packet( 40, SizeUnit.KILOBYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        Agent server = new ServerAgent( 1 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void example1() throws IOException, SimulatorException
    {
        /*
               70Mb,5ms         50Mb,3ms
        client --------> switch --------> server
         10ms             7ms              5ms
        */
        
    	NetworkTopology net = new NetworkTopology( "Topology/Topology_ex1.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                   new Time( 5,  TimeUnit.SECONDS ),
                                                   new Packet( 40, SizeUnit.KILOBYTE ),
                                                   new Packet( 40, SizeUnit.KILOBYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        Agent server = new ServerAgent( 2 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void example2() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms         50Mb,3ms
        client1 <--------> switch <--------> client2
         10ms               7ms                5ms
        */
        
    	// Use 2 active generators, in a bi-directional graph.
    	NetworkTopology net = new NetworkTopology( "Topology/Topology_ex2.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                   new Time( 5,  TimeUnit.SECONDS ),
                                                   new Packet( 40, SizeUnit.KILOBYTE ),
                                                   new Packet( 40, SizeUnit.KILOBYTE ) );
        Agent client1 = new ClientAgent( 0, generator );
        net.addAgent( client1 );
        
        CBRGenerator generator2 = new CBRGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                    new Time( 1, TimeUnit.SECONDS ),
                                                    new Packet( 20, SizeUnit.KILOBYTE ),
                                                    new Packet( 40, SizeUnit.KILOBYTE ) );
        Agent client2 = new ClientAgent( 2, generator2 );
        net.addAgent( client2 );
        
        client1.connect( client2 );
        client2.connect( client1 );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void example3() throws IOException, SimulatorException
    {
        /*
        client1 \                              / server1
          10ms	 \ 70Mb,5ms         100Mb,2ms /    5ms
                  \                          /
                   switch1 ---------- switch2
                  /  7ms    50Mb,3ms   5ms   \
                 / 70Mb,5ms          80Mb,3ms \
        client2 /                              \ server2
         10ms									   5ms
        */
        
    	NetworkTopology net = new NetworkTopology( "Topology/Topology_ex3.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator1 = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                    new Time( 5,  TimeUnit.SECONDS ),
                                                    new Packet( 40, SizeUnit.KILOBYTE ),
                                                    new Packet( 40, SizeUnit.KILOBYTE ) );
        Agent client1 = new ClientAgent( 0, generator1 );
        net.addAgent( client1 );
        
        CBRGenerator generator2 = new CBRGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                    new Time( 1, TimeUnit.SECONDS ),
                                                    new Packet( 20, SizeUnit.KILOBYTE ),
                                                    new Packet( 40, SizeUnit.KILOBYTE ) );
        Agent client2 = new ClientAgent( 1, generator2 );
        net.addAgent( client2 );
        
        Agent server1 = new ServerAgent( 4 );
        net.addAgent( server1 );
        
        Agent server2 = new ServerAgent( 5 );
        net.addAgent( server2 );
        
        client1.connect( server1 );
        client2.connect( server2 );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void example4() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms          50Mb,3ms
        client <--------> switch <--------> server
         10ms              7ms               5ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex4.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                   new Time( 5,  TimeUnit.SECONDS ),
                                                   new Packet( 40, SizeUnit.KILOBYTE ),
                                                   new Packet( 20, SizeUnit.KILOBYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        SinkGenerator generator2 = new SinkGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                      Packet.DYNAMIC,
                                                      Packet.DYNAMIC );
        Agent server = new ResponseServerAgent( 2, generator2 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void example5() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms          50Mb,3ms
        client <--------> switch <--------> server
         10ms              7ms               5ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex5.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator( new Time( 2, TimeUnit.SECONDS ),
                                                         2L,
                                                         new Packet( 40, SizeUnit.KILOBYTE ),
                                                         new Packet( 20, SizeUnit.KILOBYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        SinkGenerator generator2 = new SinkGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                      Packet.DYNAMIC,
                                                      Packet.DYNAMIC );
        Agent server = new ResponseServerAgent( 2, generator2 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void example6() throws IOException, SimulatorException
    {
        /*
        In case of multicast node it sends the request to all the possible destinations,
        waits for all the answers and replay (in case) with just one message to the input node.
        
                                   / server1
                        100Mb,2ms /  dynamic
                70Mb,5ms         /
        client ---------- switch
         10ms               7ms  \
                         80Mb,3ms \
                                   \ server2
                                       6ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex6.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator( new Time( 2, TimeUnit.SECONDS ),
                                                         1L,
                                                         new Packet( 40, SizeUnit.KILOBYTE ),
                                                         new Packet( 20, SizeUnit.KILOBYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        SinkGenerator generator2 = new SinkGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                      Packet.DYNAMIC,
                                                      Packet.DYNAMIC );
        Agent server1 = new ResponseServerAgent( 2, generator2 );
        net.addAgent( server1 );
        
        Agent server2 = new ResponseServerAgent( 3, generator2 );
        net.addAgent( server2 );
        
        MulticastGenerator switchGenerator = new MulticastGenerator( new Time( 3, TimeUnit.SECONDS ),
                                                                     1L,
                                                                     new Packet( 40, SizeUnit.KILOBYTE ),
                                                                     new Packet( 20, SizeUnit.KILOBYTE ) );
        Agent Switch = new ClientAgent( 1, switchGenerator );
        net.addAgent( Switch );
        Switch.connect( server1 );
        Switch.connect( server2 );
        
        client.connect( Switch );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void example7() throws IOException, SimulatorException
    {
        /*
        In case of multicast node it sends the request to all the possible destinations,
        waits for all the answers and replay (in case) with just one message to the input node.
        
                                   / server1
                        100Mb,2ms /  dynamic
                70Mb,5ms         /
        client ---------- switch
         10ms               7ms  \
                         80Mb,3ms \
                                   \ server2
                                       6ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex6.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator( new Time( 2, TimeUnit.SECONDS ),
                                                         1L,
                                                         new Packet( 40, SizeUnit.KILOBYTE ),
                                                         new Packet( 20, SizeUnit.KILOBYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        NetworkSettings settings = net.getNode( 0 ).getNetworkSettings();
        settings.setNetworkProtocol( new RIP( net, client ), NetworkLayer.NETWORK );
        
        SinkGenerator generator2 = new SinkGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                      Packet.DYNAMIC,
                                                      Packet.DYNAMIC );
        Agent server1 = new ResponseServerAgent( 2, generator2 );
        net.addAgent( server1 );
        
        Agent server2 = new ResponseServerAgent( 3, generator2 );
        net.addAgent( server2 );
        
        MulticastGenerator switchGenerator = new MulticastGenerator( new Time( 3, TimeUnit.SECONDS ),
                                                                     1L,
                                                                     new Packet( 40, SizeUnit.KILOBYTE ),
                                                                     new Packet( 20, SizeUnit.KILOBYTE ) );
        Agent Switch = new ClientAgent( 1, switchGenerator );
        net.addAgent( Switch );
        Switch.connect( server1 );
        Switch.connect( server2 );
        
        client.connect( Switch );
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
    }
    
    public static void testNetworkAnimation() throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Topology_test.json" );
        net.setTrackingEvent( "./Results/packets.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( Time.INFINITE, 1L,
                                                        new Packet( 20, SizeUnit.MEGABYTE ),
                                                        new Packet( 20, SizeUnit.MEGABYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator anyGen = new MulticastGenerator( Time.INFINITE, 1L,
                                                        new Packet( 20, SizeUnit.MEGABYTE ),
                                                        new Packet( 20, SizeUnit.MEGABYTE ) );
        Agent switchAgent = new SwitchAgent( 1, anyGen );
        net.addAgent( switchAgent );
        
        client.connect( switchAgent );
        for (int i = 0; i < CPU_CORES; i++) {
            Agent agentCore = new ServerAgent( 2 + i );
            net.addAgent( agentCore );
            
            switchAgent.connect( agentCore );
        }
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
        
        // Show the animation.
        AnimationNetwork an = new AnimationNetwork( 800, 600, "Distributed Network" );
        an.loadSimulation( "Topology/Topology_test.json", "./Results/packets.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();
    }
}