/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.BufferedReader;
import java.io.Closeable;
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

public class Simulator implements Closeable
{
    private NetworkTopology _network;
    private EventScheduler _evtScheduler;
    
    private List<SimulatorExecution> simExes;
    
    
    
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
        PropertyConfigurator.configure( ResourceLoader.getResourceAsStream( "Settings/log4j.properties" ) );
        BasicConfigurator.configure();
        
        simExes = new ArrayList<>();
        
        if (networks != null) {
            for (NetworkTopology net : networks) {
                setNetwork( net );
            }
        }
    }
    
    private static List<NetworkTopology> build( final String filename ) throws IOException
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
    public void setNetwork( final NetworkTopology net )
    {
        net.computeShortestPaths();
        _network = net;
        
        _evtScheduler = new EventScheduler( net );
        net.setEventScheduler( _evtScheduler );
    }
    
    /*public NetworkTopology getNetwork( final long netId ) {
        return _networks.get( netId );
    }
    
    public Collection<NetworkTopology> getNetworks() {
        return _networks.values();
    }
    
    public void trackEvents( final String eventsFile, final long netID ) throws FileNotFoundException {
        _networks.get( netID ).setTrackingEvent( eventsFile );
    }*/
    
    /**
     * Starts the simulation.
     * 
     * @param parExe    {@code true} lets the simulation to start in parallel,
     *                  {@code false} otherwise.
    */
    public void start( final boolean parExe ) {
        start( Time.INFINITE, parExe );
    }
    
    /**
     * Starts the simulation.
     * 
     * @param duration    lifetime of the simulation.
     * @param parExe      {@code true} lets the simulation to start in parallel,
     *                    {@code false} otherwise.
    */
    public void start( final Time duration, final boolean parExe )
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
        SimulatorExecution simExe = new SimulatorExecution( _network, duration );
        simExes.add( simExe );
        simExe.start();
        
        if (!parExe) {
            try {
                simExe.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            _network.shutdown();
        }
    }
    
    /*public void pause()
    {
        elapsedTime += System.currentTimeMillis() - currentTime;
        // TODO crea un evento in modo che se letto rimanga in attesa del resume.
        throw new UnsupportedOperationException( "Not yet implemented." );
    }
    
    public void resume()
    {
        currentTime = System.currentTimeMillis();
        // TODO crea e aggiunge l'evento resume in modo che riparta.
        throw new UnsupportedOperationException( "Not yet implemented." );
    }
    
    public void stop()
    {
        elapsedTime += System.currentTimeMillis() - currentTime;
        long hours   =  elapsedTime/3600000L;
        long minutes = (elapsedTime - hours*3600000L)/60000L;
        long seconds = (elapsedTime - hours*3600000L - minutes*60000L)/1000L;
        long millis  =  elapsedTime - hours*3600000L - minutes*60000L - seconds*1000L;
        Utils.LOGGER.info( "Simulation completed in " + hours + "h:" + minutes + "m:" + seconds + "s:" + millis + "ms" );
        
        for (NetworkTopology net : _networks.values()) {
            net.shutdown();
        }
    }*/
    
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
        private long currentTime = 0L;
        
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
            Utils.LOGGER.info( "Simulation start!" );
            currentTime = System.currentTimeMillis();
            net.getEventScheduler().doAllEvents();
            
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
