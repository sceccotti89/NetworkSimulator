
package simulator.core;

import simulator.coordinator.EventHandler;
import simulator.network.NetworkTopology;

public class Simulator
{
    private NetworkTopology _network;
    
    private EventHandler _evHandler;
    
    
    public Simulator()
    {
        _evHandler = new EventHandler( _network );
    }
    
    public void addNetworkTopology( final NetworkTopology network ) {
        _network = network;
    }
    
    public void start()
    {
        // TODO
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