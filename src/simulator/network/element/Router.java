/**
 * @author Stefano Ceccotti
*/

package simulator.network.element;

import simulator.core.Agent;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.network.protocols.impl.RIP;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public class Router extends Agent
{
    public Router( NetworkNode node, NetworkTopology net )
    {
        super( NetworkAgent.FULL_DUPLEX, NetworkLayer.NETWORK, node );
        
        // By default it executes the RIP protocol.
        _settings.addRoutingProtocol( new RIP( net ) );
    }
    
    /*@Override
    public long start() {
        
        return 0;
    }
    
    @Override
    public long receivedMessage( Packet message, Connection conn )
    {
        // TODO preso l'header dovrebbe semplicemente instradarlo verso il prossimo link
        return 0;
    }

    @Override
    public long notifyEvent( Event event ) {
        return 0;
    }*/

    @Override
    public String toString() {
        return "{Router - " + super.toString().substring( 1 );
    }
}
