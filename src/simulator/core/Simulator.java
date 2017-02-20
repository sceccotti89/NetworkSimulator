
package simulator.core;

import java.beans.EventHandler;

import simulator.network.NetworkTopology;

public class Simulator
{
    private NetworkTopology _network;
    
    private EventHandler ev_handler;
    
    
    public Simulator()
    {
        
    }
    
    public void addNetworkTopology( final NetworkTopology network ) {
        _network = network;
    }
    
    public void start()
    {
        // TODO
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