
package simulator.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Simulator;
import simulator.events.Event;
import simulator.events.EventGenerator;
import simulator.events.Packet;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.topology.NetworkTopology;
import simulator.utils.SizeUnit;
import simulator.utils.Time;

public class TestErrors
{
    private static class ClientGenerator extends EventGenerator
    {
        private static final String TRACE = "Models/test_arrivals.txt";
        
        private BufferedReader reader;
        private boolean closed = false;
        
        private long lastDeparture = 0;
        
        public ClientGenerator() throws IOException
        {
            super( Time.INFINITE, Time.ZERO );
            
            reader = new BufferedReader( new FileReader( TRACE ) );
        }
        
        @Override
        public Time computeDepartureTime( Event e )
        {
            if (!closed) {
                try {
                    String queryLine = null;
                    if ((queryLine = reader.readLine()) != null) {
                        long time = Long.parseLong( queryLine );
                        long timeDistance = time - lastDeparture;
                        lastDeparture = time;
                        return new Time( timeDistance, TimeUnit.MILLISECONDS );
                    } else {
                        // No more lines to read.
                        reader.close();
                        closed = true;
                    }
                } catch ( IOException e1 ) {
                    e1.printStackTrace();
                }
            }
            
            return null;
        }
    }
    
    private static class ClientAgent extends Agent
    {
        public ClientAgent( long id, EventGenerator generator )
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( generator );
        }
        
        @Override
        public void notifyEvent( Event e )
        {
            // Send the packet to the server.
            Agent dest = getConnectedAgent( 1 );
            sendMessage( dest, new Packet( 20, SizeUnit.BYTE ), true );
        }
    }
    
    private static class ServerAgent extends Agent
    {
        public ServerAgent( long id ) {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
        }
        
        @Override
        public void addEventOnQueue( Event e ) {
            System.out.println( e.getArrivalTime() + ": RICEVUTO EVENTO!" );
        }
        
        @Override
        public void notifyEvent( Event e ) {
            // Empty body.
        }
    }
    
    public static void main( String argv[] ) throws Exception
    {
        /*
                1Gb/s,0ms
        client ----------> server
          0ms
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_multiCore.json" );
        System.out.println( net.toString() );
        
        // FIXME Nel test arrival gli eventi vengono generati al tempo 1,2,3,4,5
        // FIXME pero' vengono ricevuti dal server al tempo 0,1,2,3,4 (non ci sono ritardi di rete)
        // FIXME il metodo "computeDepartureTime" del client ritorna il tempo trascorso tra 2 differenti eventi e va bene.
        // FIXME il problema e' nel metodo "generate" della classe "EventGenerator" (infatti ho messo un FIXME pure li').
        
        Simulator sim = new Simulator( net );
        
        ClientGenerator generator = new ClientGenerator();
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        Agent server = new ServerAgent( 1 );
        net.addAgent( server );
        
        client.connect( server );
        
        sim.start( new Time( 1, TimeUnit.SECONDS ), false );
        sim.close();
    }
}