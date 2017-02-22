/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.util.ArrayList;
import java.util.List;

import simulator.Agent;
import simulator.coordinator.Event;
import simulator.coordinator.EventHandler;
import simulator.network.NetworkTopology;

public class Simulator
{
    private NetworkTopology _network;
    private EventHandler _evHandler;
    private List<Agent> _agents;
    
    public Simulator()
    {
        _evHandler = new EventHandler( _network );
        _agents = new ArrayList<>();
    }
    
    public void addNetworkTopology( final NetworkTopology network ) {
        _network = network;
    }
    
    public void addAgent( final Agent agent ) {
        // TODO devo controllare che ogni id sia presente nella lista di nodi della topologia??
        _agents.add( agent );
    }
    
    public void addAgents( final List<Agent> agents ) {
        _agents.addAll( agents );
    }

    public void start()
    {
        // Put the first message into the queue.
        for(Agent agent : _agents) {
            Event e = agent.firstEvent( _evHandler );
            if(e != null)
                _evHandler.schedule( e );
        }
        
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