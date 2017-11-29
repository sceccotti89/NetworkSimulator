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
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;

public class RIP extends /*ApplicationLayerProtocol implements*/ RoutingProtocol
{
    //private static final Time UPDATE_TIME = new Time( 30, TimeUnit.SECONDS );
    
    public RIP( final NetworkTopology net )
    {
        super( NetworkLayer.APPLICATION, net );
        setNextProtocol( new UDP( 520 ) );
    }
    
    @Override
    public NetworkNode getNextNode( final long destID ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    /*@Override
    public ProtocolEvent getEvent()
    {
        // TODO generare un pacchetto contenente il contenuto della propria tabella di routing.
        //makePacket();
        ProtocolEvent event = new ProtocolEvent( time.clone(), source, getID() );
        time.addTime( UPDATE_TIME );
        return event;
        
        return null;
    }*/
    
    @Override
    public List<Header> makeHeader( final Header upperHeader, final ConnectionInfo info ) {
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
        if (extended) return "Routing Information Protocol";
        else return "RIP";
    }
}