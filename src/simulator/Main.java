
package simulator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.coordinator.Event;
import simulator.coordinator.Event.RequestEvent;
import simulator.coordinator.EventGenerator;
import simulator.core.Simulator;
import simulator.core.Time;
import simulator.exception.SimulatorException;
import simulator.network.NetworkTopology;
import simulator.utils.SimulatorUtils;
import simulator.utils.SimulatorUtils.Size;

public class Main
{
    protected static class CBRGenerator extends EventGenerator
    {
        public CBRGenerator( final Time duration,
                             final Time departureTime,
                             final Packet reqPacket,
                             final Packet resPacket )
        {
            super( duration, departureTime, SimulatorUtils.INFINITE, reqPacket, resPacket, true, false, false );
        }
        
        @Override
        public void update() {
            super.update();
        }
        
        @Override
        public Packet makePacket( final Event e ) {
            return null;
        }
    }
    
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
        public Packet makePacket( final Event e ) {
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
        }
        
        @Override
        public Packet makePacket( final Event e ) {
            return null;
        }
    }
    
    protected static class SinkGenerator extends EventGenerator
    {
        public SinkGenerator( final Time duration,
                              final Packet reqPacket,
                              final Packet resPacket )
        {
            super( duration, Time.ZERO, 1L, reqPacket, resPacket, false, false, true );
        }
        
        @Override
        public Packet makePacket( final Event e ) {
            if (e instanceof RequestEvent) {
                return new Packet( 20, Size.KB );
            } else {
                return new Packet( 40, Size.KB );
            }
        }
    }
    
    protected static class ClientAgent extends Agent
    {
        public ClientAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
        
        @Override
        public void analyzePacket( final Packet p ) {
            // TODO Auto-generated method stub
        }
    }
    
    protected static class ServerAgent extends Agent
    {
        public ServerAgent( final long id )
        {
            super( id );
        }
        
        @Override
        public void analyzePacket( final Packet p ) {
            // TODO Auto-generated method stub
        }
    }
    
    protected static class ResponseServerAgent extends Agent
    {
        public ResponseServerAgent( final long id, final EventGenerator generator )
        {
            super( id, generator );
        }
        
        @Override
        public void analyzePacket( final Packet p ) {
            // TODO Auto-generated method stub
        }
    }
    
    public static void main( final String argv[] ) throws Exception
    {
    	//example1();
    	//example2();
    	//example3();
        //example4();
        //example5();
        example6();
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
                                                   new Packet( 40, Size.KB ),
                                                   new Packet( 40, Size.KB ) );
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
                                                   new Packet( 40, Size.KB ),
                                                   new Packet( 40, Size.KB ) );
        Agent client1 = new ClientAgent( 0, generator );
        sim.addAgent( client1 );
        
        CBRGenerator generator2 = new CBRGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                    new Time( 1, TimeUnit.SECONDS ),
                                                    new Packet( 20, Size.KB ),
                                                    new Packet( 40, Size.KB ) );
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
        client1 \                              / server1
          10ms	 \ 70Mb,5ms         100Mb,2ms /    5ms
                  \                          /
                   switch1 ---------- switch2
                  /  7ms    50Mb,3ms   5ms   \
                 / 70Mb,5ms          80Mb,3ms \
        client2 /                              \ server2
         10ms									   5ms
        */
        
    	NetworkTopology net = new NetworkTopology( "Settings/Topology_ex3.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator1 = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                    new Time( 5,  TimeUnit.SECONDS ),
                                                    new Packet( 40, Size.KB ),
                                                    new Packet( 40, Size.KB ) );
        Agent client1 = new ClientAgent( 0, generator1 );
        sim.addAgent( client1 );
        
        CBRGenerator generator2 = new CBRGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                    new Time( 1, TimeUnit.SECONDS ),
                                                    new Packet( 20, Size.KB ),
                                                    new Packet( 40, Size.KB ) );
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
    
    public static void example4() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms          50Mb,3ms
        client <--------> switch <--------> server
         10ms              7ms               5ms
        */
        
        NetworkTopology net = new NetworkTopology( "Settings/Topology_ex4.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CBRGenerator generator = new CBRGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                   new Time( 5,  TimeUnit.SECONDS ),
                                                   new Packet( 40, Size.KB ),
                                                   new Packet( 20, Size.KB ) );
        Agent client = new ClientAgent( 0, generator );
        sim.addAgent( client );
        
        SinkGenerator generator2 = new SinkGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                      Packet.DYNAMIC,
                                                      Packet.DYNAMIC );
        Agent server = new ResponseServerAgent( 2, generator2 );
        sim.addAgent( server );
        
        client.connect( server );
        
        sim.start();
        
        sim.stop();
    }
    
    public static void example5() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms          50Mb,3ms
        client <--------> switch <--------> server
         10ms              7ms               5ms
        */
        
        NetworkTopology net = new NetworkTopology( "Settings/Topology_ex5.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator( new Time( 2, TimeUnit.SECONDS ),
                                                         2L,
                                                         new Packet( 40, Size.KB ),
                                                         new Packet( 20, Size.KB ) );
        Agent client = new ClientAgent( 0, generator );
        sim.addAgent( client );
        
        SinkGenerator generator2 = new SinkGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                      Packet.DYNAMIC,
                                                      Packet.DYNAMIC );
        Agent server = new ResponseServerAgent( 2, generator2 );
        sim.addAgent( server );
        
        client.connect( server );
        
        sim.start();
        
        sim.stop();
    }
    
    public static void example6() throws IOException, SimulatorException
    {
        /*
                                   / server1
                        100Mb,2ms /    5ms
                70Mb,5ms         /
        client ---------- switch
         10ms               7ms  \
                         80Mb,3ms \
                                   \ server2
                                       6ms
        */
        
        NetworkTopology net = new NetworkTopology( "Settings/Topology_ex6.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator( new Time( 2, TimeUnit.SECONDS ),
                                                         1L,
                                                         new Packet( 40, Size.KB ),
                                                         new Packet( 20, Size.KB ) );
        Agent client = new ClientAgent( 0, generator );
        sim.addAgent( client );
        
        SinkGenerator generator2 = new SinkGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                      Packet.DYNAMIC,
                                                      Packet.DYNAMIC );
        Agent server1 = new ResponseServerAgent( 2, generator2 );
        sim.addAgent( server1 );
        
        Agent server2 = new ResponseServerAgent( 3, generator2 );
        sim.addAgent( server2 );
        
        MulticastGenerator switchGenerator = new MulticastGenerator( new Time( 2, TimeUnit.SECONDS ),
                                                                     1L,
                                                                     new Packet( 40, Size.KB ),
                                                                     new Packet( 20, Size.KB ) );
        Agent Switch = new ClientAgent( 1, switchGenerator );
        sim.addAgent( Switch );
        Switch.connect( server1 );
        Switch.connect( server2 );
        
        client.connect( Switch );
        
        sim.start();
        
        sim.stop();
    }
}