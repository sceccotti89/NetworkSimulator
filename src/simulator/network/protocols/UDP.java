/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.events.impl.ProtocolEvent;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public class UDP extends NetworkProtocol
{
    public UDP( final NetworkTopology net, final Agent agent ) {
        super( net, agent );
    }
    
    public UDP( final NetworkTopology net, final Agent agent, final int port )
    {
        super( net, agent );
        setPort( port );
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
        return "UDP";
    }
}