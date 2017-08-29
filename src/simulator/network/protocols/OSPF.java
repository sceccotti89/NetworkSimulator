/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.events.impl.ProtocolEvent;
import simulator.network.NetworkLayer;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public class OSPF extends NetworkProtocol
{
    public OSPF( final NetworkTopology net, final Agent agent ) {
        super( net, agent );
        setLayer( NetworkLayer.NETWORK.getIndex() );
        // TODO viene incapsulato direttamente in un pacchetto IP con protocollo 89.
        
    }
    
    @Override
    public NetworkNode getNextNode( final long destID ) {
        // TODO Auto-generated method stub
        return null;
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
    public Event processPacket( final Packet packet ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getID() {
        return "OSPF";
    }
}