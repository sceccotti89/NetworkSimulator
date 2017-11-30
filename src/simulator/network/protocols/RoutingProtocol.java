/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.network.NetworkLayer;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public abstract class RoutingProtocol extends Protocol
{
    public RoutingProtocol( NetworkLayer layer, NetworkTopology net, Protocol... baseProtocols ) {
        super( layer, net, baseProtocols );
    }
    
    /**
     * Returns the next node starting from the current one.
     * 
     * @param destID    destination node address.
     * 
     * @return the next node in the path.
    */
    public abstract NetworkNode getNextNode( long destID );
    //TODO public abstract NetworkNode getNextNode( String destination );
}
