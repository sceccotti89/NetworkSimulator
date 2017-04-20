/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import simulator.Agent;
import simulator.exception.SimulatorException;
import simulator.manager.EventScheduler;
import simulator.network.NetworkTopology;

public class Simulator
{
    private NetworkTopology _network;
    private EventScheduler _evtScheduler;
    private List<Agent> _agents;
    
    public Simulator()
    { this( (NetworkTopology) null ); }
    
    public Simulator( final String filename ) throws IOException, SimulatorException
    { this( new NetworkTopology( filename ) ); }
    
    public Simulator( final NetworkTopology network )
    {
        PropertyConfigurator.configure( "Settings/log4j.properties" );
        BasicConfigurator.configure();
        
        _network      = network;
        _evtScheduler = new EventScheduler( network );
        _agents       = new ArrayList<>();
    }
    
    public void setNetworkTopology( final NetworkTopology network ) {
        _network = network;
    }
    
    public void addAgent( final Agent agent ) throws SimulatorException
    {
        if (_network == null)
            throw new SimulatorException( "A network topology is not setted up." );
        if (!_network.containsNode( agent.getId() ))
            throw new SimulatorException( "No node found for ID: " + agent.getId() );
        
        agent.setNode( _network.getNode( agent.getId() ) );
        agent.setEventScheduler( _evtScheduler );
        _agents.add( agent );
    }
    
    public void addAgents( final List<Agent> agents ) {
        _agents.addAll( agents );
    }
    
    public void start() {
        start( Time.INFINITE );
    }
    
    public void start( final Time duration )
    {
        _network.computeShortestPaths();
        
        _evtScheduler.setNetworkTopology( _network );
        _evtScheduler.setDuration( duration );
        
        // Put the first message into the queue.
        for (Agent agent : _agents) {
            _evtScheduler.schedule( agent.fireEvent( null, null ) );
        }
        
        _evtScheduler.doAllEvents();
    }
    
    public void pause()
    {
        // TODO
    }
    
    public void resume()
    {
        // TODO
    }
    
    public void stop()
    {
        // TODO close also the event handler.
        //_evHandler.shutdown();
        //_network.shutdown();
        
        for (Agent agent : _agents) {
            agent.shutdown();
        }
    }
}