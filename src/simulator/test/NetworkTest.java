
package simulator.test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Simulator;
import simulator.events.Event;
import simulator.events.EventGenerator;
import simulator.events.EventHandler;
import simulator.events.Packet;
import simulator.exception.SimulatorException;
import simulator.graphics.AnimationNetwork;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.topology.NetworkTopology;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class NetworkTest
{
    private static final int CPU_CORES = 4;
    
    private static class ClientGenerator extends EventGenerator
    {
        public ClientGenerator( Time duration ) {
            super( duration, Time.ZERO, EventGenerator.BEFORE_CREATION );
        }
    }
    
    private static class MulticastGenerator extends EventGenerator
    {
        public MulticastGenerator( Time duration )
        {
            super( duration, Time.ZERO, EventGenerator.BEFORE_CREATION );
        }
    }
    
    private static class SwitchAgent extends Agent
    {
        public SwitchAgent( long id, EventGenerator evGenerator )
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
        }
        
        @Override
        public double getNodeUtilization( Time time )
        {
            double utilization = 0;
            for (Agent agent : getConnectedAgents()) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
        }
        
        @Override
        public void notifyEvent( Event e ) {
            // TODO Auto-generated method stub
            
        }
    }
    
    private static class ClientAgent extends Agent
    {
        public ClientAgent( long id, EventGenerator evGenerator )
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
        }
        
        @Override
        public void notifyEvent( Event e ) {
            // TODO Auto-generated method stub
            
        }
    }
    
    private static class ServerAgent extends Agent
    {
        public ServerAgent( long id ) {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
        }
        
        @Override
        public void notifyEvent( Event e ) {
            // TODO Auto-generated method stub
            
        }
    }
    
    private static class ResponseServerAgent extends Agent
    {
        public ResponseServerAgent( long id, EventGenerator generator )
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( generator );
        }
        
        @Override
        public void notifyEvent( Event e ) {
            // TODO Auto-generated method stub
            
        }
    }
    
    public static void main( String argv[] ) throws Exception
    {
        Utils.VERBOSE = false;
    	//example1();
    	//example2();
    	//example3();
        //example4();
        //example5();
        //example6();
        //example7();
        example8();
        //testNetworkAnimation();
    	
    	System.out.println( "End of simulation." );
    }
    
    public static void example1() throws IOException, SimulatorException
    {
        /*
               70Mb,5ms
        client --------> server
         10ms             5ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex1.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new EventGenerator( new Time( 20, TimeUnit.SECONDS ),
                                                       new Time( 5,  TimeUnit.SECONDS ),
                                                       EventGenerator.BEFORE_CREATION );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        Agent server = new ServerAgent( 1 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
    }
    
    public static void example2() throws IOException, SimulatorException
    {
        /*
               70Mb,5ms         50Mb,3ms
        client --------> switch --------> server
         10ms             7ms              5ms
        */
        
    	NetworkTopology net = new NetworkTopology( "Topology/Topology_ex2.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new EventGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                       new Time( 5,  TimeUnit.SECONDS ),
                                                       EventGenerator.BEFORE_CREATION );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        Agent server = new ServerAgent( 2 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
    }
    
    public static void example3() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms         50Mb,3ms
        client1 <--------> switch <--------> client2
         10ms               7ms                5ms
        */
        
    	// Use 2 active generators, in a bi-directional graph.
    	NetworkTopology net = new NetworkTopology( "Topology/Topology_ex3.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new EventGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                       new Time( 5,  TimeUnit.SECONDS ),
                                                       EventGenerator.BEFORE_CREATION );
        Agent client1 = new ClientAgent( 0, generator );
        net.addAgent( client1 );
        
        EventGenerator generator2 = new EventGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                        new Time( 1, TimeUnit.SECONDS ),
                                                        EventGenerator.BEFORE_CREATION );
        Agent client2 = new ClientAgent( 2, generator2 );
        net.addAgent( client2 );
        
        client1.connect( client2 );
        client2.connect( client1 );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
    }
    
    public static void example4() throws IOException, SimulatorException
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
        
    	NetworkTopology net = new NetworkTopology( "Topology/Topology_ex4.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator1 = new EventGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                        new Time( 5,  TimeUnit.SECONDS ),
                                                        EventGenerator.BEFORE_CREATION );
        Agent client1 = new ClientAgent( 0, generator1 );
        net.addAgent( client1 );
        
        EventGenerator generator2 = new EventGenerator( new Time( 5, TimeUnit.SECONDS ),
                                                        new Time( 1, TimeUnit.SECONDS ),
                                                        EventGenerator.BEFORE_CREATION );
        Agent client2 = new ClientAgent( 1, generator2 );
        net.addAgent( client2 );
        
        Agent server1 = new ServerAgent( 4 );
        net.addAgent( server1 );
        
        Agent server2 = new ServerAgent( 5 );
        net.addAgent( server2 );
        
        client1.connect( server1 );
        client2.connect( server2 );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
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
        
        EventGenerator generator = new EventGenerator( new Time( 10, TimeUnit.SECONDS ),
                                                       new Time( 5,  TimeUnit.SECONDS ),
                                                       EventGenerator.BEFORE_CREATION );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator generator2 = new EventGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                        Time.ZERO,
                                                        EventGenerator.BEFORE_CREATION );
        Agent server = new ResponseServerAgent( 2, generator2 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
    }
    
    public static void example6() throws IOException, SimulatorException
    {
        /*
                70Mb,5ms          50Mb,3ms
        client <--------> switch <--------> server
         10ms              7ms               5ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex6.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator( new Time( 2, TimeUnit.SECONDS ) );
        //generator.setMaximumFlyingPackets( 2 );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator generator2 = new EventGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                        Time.ZERO,
                                                        EventGenerator.BEFORE_CREATION );
        Agent server = new ResponseServerAgent( 2, generator2 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
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
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex7.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator( new Time( 2, TimeUnit.SECONDS ) );
        //generator.setWaitForResponse( true );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator generator2 = new EventGenerator( new Time( 15, TimeUnit.SECONDS ),
                                                        Time.ZERO,
                                                        EventGenerator.BEFORE_CREATION );
        Agent server1 = new ResponseServerAgent( 2, generator2 );
        net.addAgent( server1 );
        
        Agent server2 = new ResponseServerAgent( 3, generator2 );
        net.addAgent( server2 );
        
        MulticastGenerator switchGenerator = new MulticastGenerator( new Time( 3, TimeUnit.SECONDS ) );
        Agent Switch = new ClientAgent( 1, switchGenerator );
        net.addAgent( Switch );
        Switch.connect( server1 );
        Switch.connect( server2 );
        
        client.connect( Switch );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
    }
    
    
    
    
    
    private static class ClientAgent2 extends Agent implements EventHandler
    {
        public ClientAgent2( long id, EventGenerator evGenerator )
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
        }
        
        @Override
        public void notifyEvent( Event e )
        {
            System.out.println( "\n\nEVENT: " + e );
            Agent dest = getConnectedAgent( 1 );
            Packet message = new Packet( 20, SizeUnit.BYTE );
            sendMessage( dest, message, true );
        }
        
        @Override
        public void receivedMessage( Event e ) {
            System.out.println( "[CLIENT] Ricevuto: " + e );
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            System.out.println( "HANDLING:" + e + ", TYPE: " + type );
            return getNode().getTcalc();
        }
    }
    
    private static class SwitchAgent2 extends Agent
    {
        private int received = 0;
        
        public SwitchAgent2( long id ) {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
        }

        @Override
        public void notifyEvent( Event e ) {}
        
        @Override
        public void receivedMessage( Event e )
        {
            System.out.println( "[SWITCH] RICEVUTO: " + e );
            if (e.getSource().getId() == 0) {
                // From client.
                System.out.println( "DAL CLIENT.." );
                for (Agent agent : getConnectedAgents()) {
                    if (agent.getId() != 0) {
                        sendMessage( agent, e.getPacket(), true );
                    }
                }
            } else {
                // From server.
                System.out.println( "DAL SERVER.." );
                received = (received + 1) % 2;
                if (received == 0) {
                    // Send back a message to the client.
                    Agent dest = getConnectedAgent( 0 );
                    Packet message = new Packet( 40, SizeUnit.BYTE );
                    sendMessage( dest, message, false );
                }
            }
        }
    }
    
    private static class ServerAgent2 extends Agent
    {
        public ServerAgent2( long id ) {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
        }
        
        @Override
        public void notifyEvent( Event e ) {}
        
        @Override
        public void receivedMessage( Event e )
        {
            System.out.println( "[SERVER] RICEVUTO: " + e );
            Agent dest = getConnectedAgent( 1 );
            Packet message = new Packet( 40, SizeUnit.BYTE );
            sendMessage( dest, message, false );
        }
    }
    
    public static void example8() throws IOException, SimulatorException
    {
        /*
        The switch node sends the request to all the possible destinations,
        waits for all the answers and replay with just one message to the input node.
        
                                   / server1
                        100Mb,2ms /    0ms
                70Mb,5ms         /
        client ---------- switch
         10ms               7ms  \
                         80Mb,3ms \
                                   \ server2
                                       6ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_ex7.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new EventGenerator( new Time( 1, TimeUnit.SECONDS ),
                                                       new Time( 1, TimeUnit.SECONDS ),
                                                       EventGenerator.BEFORE_CREATION );
        Agent client = new ClientAgent2( 0, generator );
        net.addAgent( client );
        
        Agent switchAgent = new SwitchAgent2( 1 );
        net.addAgent( switchAgent );
        
        Agent server1 = new ServerAgent2( 2 );
        net.addAgent( server1 );
        
        Agent server2 = new ServerAgent2( 3 );
        net.addAgent( server2 );
        
        // Connect the agents.
        client.connect( switchAgent );
        switchAgent.connect( server1 );
        switchAgent.connect( server2 );
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
    }
    
    public static void testNetworkAnimation() throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Topology_animation_test.json" );
        net.setTrackingEvent( "./Results/packets.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( Time.INFINITE );
        //generator.setWaitForResponse( true );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator multiGen = new MulticastGenerator( Time.INFINITE );
        //multiGen.setWaitForResponse( true );
        Agent switchAgent = new SwitchAgent( 1, multiGen );
        net.addAgent( switchAgent );
        
        client.connect( switchAgent );
        for (int i = 0; i < CPU_CORES; i++) {
            Agent agentCore = new ServerAgent( 2 + i );
            net.addAgent( agentCore );
            
            switchAgent.connect( agentCore );
        }
        
        sim.start( new Time( 1, TimeUnit.HOURS ), false );
        sim.close();
        
        // Show the animation.
        AnimationNetwork an = new AnimationNetwork( 800, 600, "Distributed Network" );
        an.loadSimulation( "Topology/Topology_animation_test.json", "./Results/packets.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();
    }
}
