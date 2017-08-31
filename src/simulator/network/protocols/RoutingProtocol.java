/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.topology.NetworkNode;

public interface RoutingProtocol
{
    /**
     * Returns the next node starting from the current one.
     * 
     * @param destID    destination node identifier.
     * 
     * @return the next node in the graph.
    */
    public NetworkNode getNextNode( final long destID );
}