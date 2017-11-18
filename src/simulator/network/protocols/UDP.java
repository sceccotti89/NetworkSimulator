/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.events.Event;
import simulator.events.Packet;
import simulator.events.impl.ProtocolEvent;
import simulator.network.protocols.NetworkProtocol.TransportLayerProtocol;

public class UDP extends TransportLayerProtocol
{
    public UDP( int sourcePort, int destPort ) {
        super( sourcePort, destPort );
    }
    
    @Override
    public ProtocolEvent getEvent() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Packet makePacket() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Event processPacket( Packet packet ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getID() {
        return "UDP";
    }
}
