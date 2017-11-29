/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols.impl;

import java.util.LinkedHashMap;
import java.util.List;

import simulator.network.ConnectionInfo;
import simulator.network.NetworkLayer;
import simulator.network.protocols.Header;
import simulator.network.protocols.ProtocolReference;
import simulator.network.protocols.RoutingProtocol;
import simulator.network.protocols.impl.IP.IPv4;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public class OSPF extends /*extends TransportLayerProtocol*/ RoutingProtocol
{
    public OSPF( final NetworkTopology net )
    {
        super( NetworkLayer.NETWORK, net, new IPv4( 89 ) );
        // TODO utilizza un indirizzo broadcast (capire quale) per trasmettere le informazioni
    }
    
    @Override
    public NetworkNode getNextNode( final long destID ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public List<Header> makeHeader( final Header upperLayer, final ConnectionInfo info ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public ProtocolReference processHeader( final Header header ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void printHeader( final Header header ) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public LinkedHashMap<String, String> getFields( final Header header ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getName( final boolean extended ) {
        if (extended) return "Open Shortest Path First Routing Protocol";
        else return "OSPF";
    }
}