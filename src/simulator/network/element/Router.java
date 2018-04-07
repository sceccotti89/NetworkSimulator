/**
 * @author Stefano Ceccotti
*/

package simulator.network.element;

import simulator.core.Agent;
import simulator.events.Event;
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
    }*/
    
    @Override
    public void receivedMessage( Event e )
    {
        // TODO preso l'header dovrebbe semplicemente instradarlo verso il prossimo link
    }
    
    @Override
    public void notifyEvent( Event event ) {
        
    }

    @Override
    public String toString() {
        return "{Router - " + super.toString().substring( 1 );
    }
}
