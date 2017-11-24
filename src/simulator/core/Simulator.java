/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;

import simulator.events.EventScheduler;
import simulator.network.element.Switch;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;
import simulator.utils.Utils;
import simulator.utils.resources.ResourceLoader;

public class Simulator implements AutoCloseable
{
    private NetworkTopology _network;
    private EventScheduler _evtScheduler;
    
    private List<SimulatorExecution> simExes;
    
    
    
    public Simulator() {
        this( (List<NetworkTopology>) null );
    }
    
    public Simulator( String filename ) throws IOException {
        this( build( filename ) );
    }
    
    public Simulator( NetworkTopology net ) {
        this( Collections.singletonList( net ) );
    }
    
    public Simulator( List<NetworkTopology> networks )
    {
        PropertyConfigurator.configure( ResourceLoader.getResourceAsStream( "Settings/log4j.properties" ) );
        BasicConfigurator.configure();
        
        simExes = new ArrayList<>();
        
        if (networks != null) {
            for (NetworkTopology net : networks) {
                setNetwork( net );
            }
        }
    }
    
    private static List<NetworkTopology> build( String filename ) throws IOException
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
     * Sets the current network.
     * 
     * @param net    the network to set.
    */
    public void setNetwork( NetworkTopology net )
    {
        net.computeShortestPaths();
        _network = net;
        
        _evtScheduler = new EventScheduler( net );
        net.setEventScheduler( _evtScheduler );
    }
    
    /**
     * Starts the simulation.
     * 
     * @param parExe    {@code true} lets the simulation to start in parallel,
     *                  {@code false} otherwise.
    */
    public void start( boolean parExe ) throws IOException {
        start( Time.INFINITE, parExe );
    }
    
    /**
     * Starts the simulation.
     * 
     * @param duration    lifetime of the simulation.
     * @param parExe      {@code true} lets the simulation to start in parallel,
     *                    {@code false} otherwise.
    */
    public void start( Time duration, boolean parExe ) throws IOException
    {
        //List<SimulatorExecution> simExes = new ArrayList<>( _evtSchedulers.size() );
        for (NetworkNode node : _network.getNodes()) {
            if (node.getAgent() == null) {
                // By default the agent is a switch.
                node.setAgent( new Switch( node, _network ) );
                _network.addAgent( node.getAgent() );
            }
        }
        
        //_network.computeShortestPaths();
        
        EventScheduler evtScheduler = _network.getEventScheduler();
        evtScheduler.setDuration( duration );
        
        if (!parExe) {
            SimulatorExecution.startSimulation( _network );
            _network.shutdown();
        } else {
            SimulatorExecution simExe = new SimulatorExecution( _network );
            simExes.add( simExe );
            simExe.start();
        }
    }
    
    @Override
    public void close() throws IOException
    {
        for (SimulatorExecution exe : simExes) {
            try {
                exe.join();
                exe.getNetwork().shutdown();
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }
    }

    private static class SimulatorExecution extends Thread
    {
        private final NetworkTopology net;
        
        public SimulatorExecution( NetworkTopology net ) {
            this.net = net;
        }
        
        @Override
        public void run()
        {
            try {
                startSimulation( net );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        
        private static final void startSimulation( NetworkTopology net ) throws IOException
        {
            Utils.LOGGER.info( "Simulation start!" );
            long currentTime = System.currentTimeMillis();
            
            EventScheduler evtScheduler = net.getEventScheduler();
            for (Agent agent : net.getAgents()) {
                evtScheduler.schedule( agent.fireEvent( null, null ) );
            }
            
            net.getEventScheduler().doAllEvents();
            
            net.shutdown();
            
            // Calculates the time elapsed from the beginning of the simulation.
            long elapsedTime = System.currentTimeMillis() - currentTime;
            long hours   =  elapsedTime/3600000L;
            long minutes = (elapsedTime - hours*3600000L)/60000L;
            long seconds = (elapsedTime - hours*3600000L - minutes*60000L)/1000L;
            long millis  =  elapsedTime - hours*3600000L - minutes*60000L - seconds*1000L;
            Utils.LOGGER.info( "Simulation completed in " + hours + "h:" + minutes + "m:" + seconds + "s:" + millis + "ms" );
        }
        
        public NetworkTopology getNetwork() {
            return net;
        }
    }
}
