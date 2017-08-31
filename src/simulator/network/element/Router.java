/**
 * @author Stefano Ceccotti
*/

package simulator.network.element;

import simulator.core.Agent;
import simulator.network.protocols.RIP;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public class Router extends Agent
{
    public Router( final NetworkNode node, final NetworkTopology net, final long id )
    {
        super( node );
        
        // By default it executes the RIP protocol.
        node.getNetworkSettings().addRoutingProtocol( new RIP( net, this ) );
    }
    
    @Override
    public String toString() {
        return "Router - " + super.toString();
    }
}