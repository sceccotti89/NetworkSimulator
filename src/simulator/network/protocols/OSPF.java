/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.events.impl.ProtocolEvent;
import simulator.network.NetworkLayer;
import simulator.network.protocols.IP.IPv4;
import simulator.network.protocols.NetworkProtocol.TransportLayerProtocol;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public class OSPF extends TransportLayerProtocol implements RoutingProtocol
{
    public OSPF( final NetworkTopology net, final Agent agent )
    {
        super( net, agent );
        setLayer( NetworkLayer.TRANSPORT.getIndex() );
        // TODO utilizza un indirizzo broadcast (capire quale) per trasmettere le informazioni
        IP ip = new IPv4( agent.getNode().getNetworkSettings().getIPv4address(), "" );
        ip.setProtocolID( 89 );
        protocols.put( NetworkLayer.NETWORK, ip );
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