/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import simulator.Agent;
import simulator.coordinator.EventHandler;
import simulator.exception.SimulatorException;
import simulator.network.NetworkTopology;

public class Simulator
{
    private NetworkTopology _network;
    private EventHandler _evHandler;
    private List<Agent> _agents;
    
    public Simulator()
    { this( (NetworkTopology) null ); }
    
    public Simulator( final String filename ) throws IOException, SimulatorException
    { this( new NetworkTopology( filename ) ); }
    
    public Simulator( final NetworkTopology network )
    {
        _network = network;
        _evHandler = new EventHandler( network );
        _agents = new ArrayList<>();
    }
    
    public void setNetworkTopology( final NetworkTopology network ) {
        _network = network;
    }
    
    public void addAgent( final Agent agent ) {
        // TODO devo controllare che ogni id sia presente nella lista di nodi della topologia??
        _agents.add( agent );
    }
    
    public void addAgents( final List<Agent> agents ) {
        _agents.addAll( agents );
    }
    
    // TODO aggiungere l'opzione di poter lanciare la simulazione per tot secondi
    
    public void start()
    {
        _evHandler.setNetworkTopology( _network );
        
        // Put the first message into the queue.
        for(Agent agent : _agents)
            _evHandler.schedule( agent.firstEvent() );
        
        _evHandler.doAllEvents();
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
        // TODO
    }
}