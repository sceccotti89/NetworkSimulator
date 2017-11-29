/**
 * @author Stefano Ceccotti
*/

package simulator.network.element;

import simulator.core.Agent;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

/**
 * Layer 1 Hub class.
*/
public class Hub extends Agent
{
    public Hub( final NetworkNode node, final NetworkTopology net ) {
        super( NetworkAgent.HALF_DUPLEX, NetworkLayer.PHYSICAL, net, node );
    }
    
    /*@Override
    public long receivedMessage( final Packet message, final Connection conn )
    {
        // TODO Ricevuto un pacchetto in ingresso, lo invia in broadcast su tutte le altre porte.
        // TODO Per questo e' half-duplex.
        return 0;
    }
    
    @Override
    public long notifyEvent( final Event event ) {
        return 0;
    }*/

    @Override
    public String toString() {
        return "{Switch - " + super.toString().substring( 1 );
    }
}