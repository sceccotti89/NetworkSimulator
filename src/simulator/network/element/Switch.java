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

public class Switch extends Agent
{
    public Switch( NetworkNode node, NetworkTopology net )
    {
        super( NetworkAgent.FULL_DUPLEX, NetworkLayer.DATA_LINK, node );
        
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
        // TODO capire come funziona uno switch.
        //Header msg = (Header) message;
        // TODO inoltrare il messaggio verso la direzione corretta
        //conn.send( (Header) message );
        return 0;
    }
    
    @Override
    public long notifyEvent( Event event ) {
        return 0;
    }*/

    @Override
    public String toString() {
        return "{Switch - " + super.toString().substring( 1 );
    }
}
