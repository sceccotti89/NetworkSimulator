/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.network.ConnectionInfo;
import simulator.network.protocols.DataLinkProtocol;
import simulator.network.protocols.EventProtocol;
import simulator.network.protocols.Header;
import simulator.network.protocols.ProtocolReference;
import simulator.utils.Time;

public class ARP extends DataLinkProtocol implements EventProtocol
{
    // TODO questo protocollo ha al suo interno una cache in cui vengono salvati i risultati
    // TODO di default mettere il timeout a 4 ore (come nei router Cisco).
    // TODO le aggiunte static (fatte manualmente) rimagono per sempre.
    // TODO le aggiunte dynamic (fatte dal protocollo) seguono il timeout.
    private Map<String, CacheInfo> cache;
    
    private static final long TIMEOUT = new Time( 4, TimeUnit.HOURS ).getTimeMicros();
    
    
    public ARP() {
        cache = new HashMap<>();
    }
    
    @Override
    public List<Header> makeHeader( final Header upperHeader, final ConnectionInfo info )
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public ProtocolReference processHeader( final Header header )
    {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Header processEvent( final TimeoutEvent event ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public void printHeader( final Header header )
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public LinkedHashMap<String, String> getFields( final Header header ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getName( final boolean extended ) {
        if (extended) return "Address Resolution Protocol";
        else return "ARP";
    }
    
    private static class CacheInfo
    {
        private String MACaddress;
        private int type;
        
        public static final int DYNAMIC = 0;
        public static final int STATIC = 1;
        
        public CacheInfo( final String MACaddress, final int type )
        {
            this.MACaddress = MACaddress;
            this.type = type;
        }
        
        public String getMACaddress() {
            return MACaddress;
        }
        
        public int getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return "Phisical address: " + MACaddress +
                   ", Type: " + (type == DYNAMIC ? "DYNAMIC" : "STATIC");
        }
    }
}