/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;

import simulator.events.EventScheduler;
import simulator.exception.SimulatorException;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;
import simulator.utils.Utils;

public class Simulator
{
    private Map<Long,NetworkTopology> _networks;
    private Map<Long,EventScheduler> _evtSchedulers;
    
    
    
    public Simulator() {
        this( (List<NetworkTopology>) null );
    }
    
    public Simulator( final String filename ) throws IOException {
        this( build( filename ) );
    }
    
    public Simulator( final NetworkTopology net ) {
        this( Collections.singletonList( net ) );
    }
    
    public Simulator( final List<NetworkTopology> networks )
    {
        PropertyConfigurator.configure( "Settings/log4j.properties" );
        BasicConfigurator.configure();
        
        _networks = new HashMap<>();
        
        _evtSchedulers = new HashMap<>();
        
        if (networks != null) {
            for (NetworkTopology net : networks) {
                addNetwork( net );
            }
        }
    }
    
    public static List<NetworkTopology> build( final String filename ) throws IOException
    {
        List<NetworkTopology> networks = new ArrayList<>();
        
        BufferedReader br = new BufferedReader( new FileReader( filename ) );
        StringBuilder content = new StringBuilder( 512 );
        
        /**
         * File structure:
         * 
         * networks => [{[nodes],[links]}, ...]
        */
        
        String nextLine = null;
        while((nextLine = br.readLine()) != null)
            content.append( nextLine.trim() );
        
        JSONObject settings = new JSONObject( content.toString() );
        JSONArray nets = settings.getJSONArray( "networks" );
        for (int i = 0; i < nets.length(); i++) {
            networks.add( new NetworkTopology( (JSONObject) nets.get( i ) ) );
        }
        
        br.close();
        
        return networks;
    }
    
    /**
     * Adds a new network. This network will be executed in parallel with respect to
     * all the others, with no default communication mechanisms.</br>
     * To let the networks communicate with each other just create a link in one of the
     * two networks, or both if it's duplex.
     * 
     * @param net    the new network to add.
    */
    public void addNetwork( final NetworkTopology net )
    {
        // Checks the unicity of each node ID.
        for (NetworkNode node : net.getNodes()) {
            for (NetworkTopology network : _networks.values()) {
                if (network.containsNode( node.getId() )) {
                    throw new SimulatorException( "Duplicated node ID: " + node.getId() );
                }
            }
        }
        
        net.computeShortestPaths();
        _networks.put( net.getId(), net );
        
        EventScheduler evtScheduler = new EventScheduler( this, net );
        _evtSchedulers.put( net.getId(), evtScheduler );
        net.setEventScheduler( evtScheduler );
    }
    
    public NetworkTopology getNetwork( final long netId ) {
        return _networks.get( netId );
    }
    
    public Collection<NetworkTopology> getNetworks() {
        return _networks.values();
    }
    
    public void start() {
        start( Time.INFINITE );
    }
    
    public void start( final Time duration )
    {
        Utils.LOGGER.info( "Simulation start!" );
        
        // Start each network in parallel.
        List<SimulatorExecution> simExes = new ArrayList<>( _evtSchedulers.size() );
        for (EventScheduler evtScheduler : _evtSchedulers.values()) {
            NetworkTopology net = evtScheduler.getNetwork();
            //net.computeShortestPaths();
            SimulatorExecution simExe = new SimulatorExecution( net, duration );
            simExes.add( simExe );
            simExe.start();
        }
        
        for (SimulatorExecution exe : simExes) {
            try {
                exe.join();
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }
    
    public void pause()
    {
        // TODO
        throw new UnsupportedOperationException( "Not yet implemented." );
    }
    
    public void resume()
    {
        // TODO
        throw new UnsupportedOperationException( "Not yet implemented." );
    }
    
    public void stop()
    {
        for (NetworkTopology net : _networks.values()) {
            net.shutdown();
        }
    }
    
    private static class SimulatorExecution extends Thread
    {
        private final NetworkTopology net;
        
        public SimulatorExecution( final NetworkTopology net, final Time duration )
        {
            this.net = net;
            EventScheduler evtScheduler = net.getEventScheduler();
            evtScheduler.setDuration( duration );
            for (Agent agent : net.getAgents()) {
                evtScheduler.schedule( agent.fireEvent( null, null ) );
            }
        }
        
        @Override
        public void run() {
            net.getEventScheduler().doAllEvents();
        }
    }
}