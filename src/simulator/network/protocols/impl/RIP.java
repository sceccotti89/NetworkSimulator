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
    
    public RIP( NetworkTopology net )
    {
        super( NetworkLayer.APPLICATION, net );
        setNextProtocol( new UDP( 520 ) );
    }
    
    @Override
    public NetworkNode getNextNode( long destID ) {
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
    public List<Header> makeHeader( Header upperHeader, ConnectionInfo info ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public ProtocolReference processHeader( Header header ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void printHeader( Header header ) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public LinkedHashMap<String, String> getFields( Header header ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getName( boolean extended ) {
        if (extended) return "Routing Information Protocol";
        else return "RIP";
    }
}
