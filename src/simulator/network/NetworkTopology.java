
package simulator.network;

import java.util.ArrayList;
import java.util.List;

import simulator.core.NetworkNode;

public class NetworkTopology
{
    private List<NetworkLink> networkLinks = new ArrayList<>( 32 );
    private List<NetworkNode> networkNodes = new ArrayList<>( 32 );
    
    public NetworkTopology()
    {
        
    }
    
    public NetworkTopology( final String filename )
    {
        
    }
    
    private void build( final String filename )
    {
        // TODO Implementare..
    }
    
    public void addLink( final int fromId, final int destId,
                         final double bandwith, final double delay )
    {
        
    }
    
    public void addLink( final NetworkLink link ) {
        networkLinks.add( link );
    }
    
    public void addNode()
    {
        
    }
}