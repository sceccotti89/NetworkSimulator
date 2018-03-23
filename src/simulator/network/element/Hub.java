/**
 * @author Stefano Ceccotti
*/

package simulator.network.element;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.topology.NetworkNode;

/**
 * Layer 1 Hub class.
*/
public class Hub extends Agent
{
    public Hub( NetworkNode node ) {
        super( NetworkAgent.HALF_DUPLEX, NetworkLayer.PHYSICAL, node );
    }
    
    /*@Override
    public long receivedMessage( Packet message, Connection conn )
    {
        // TODO Ricevuto un pacchetto in ingresso, lo invia in broadcast su tutte le altre porte.
        // TODO Per questo e' half-duplex.
        return 0;
    }*/
    
    @Override
    public void notifyEvent( Event event ) {
        
    }

    @Override
    public String toString() {
        return "{Switch - " + super.toString().substring( 1 );
    }
}
