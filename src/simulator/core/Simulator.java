/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.util.ArrayList;
import java.util.List;

import simulator.Agent;
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
        _agents.add( agent );
    }
    
    public void addAgents( final List<Agent> agents ) {
        _agents.addAll( agents );
    }

    public void start()
    {
        // TODO gli attori che sono stati aggiunti al simulatore inseriscono il loro primo messaggio nella coda.
        // TODO stare quindi attenti a quelli che non dovrebbe inivare nulla
        
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