/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.events.Event;
import simulator.events.Packet;
import simulator.events.impl.ProtocolEvent;
import simulator.network.protocols.NetworkProtocol.NetworkLayerProtocol;
import simulator.utils.SizeUnit;

public abstract class IP extends NetworkLayerProtocol
{
    protected String protocolID;
    
    public IP( final int protocolID ) {
        setProtocolID( protocolID );
    }
    
    public void setProtocolID( final int protocolID ) {
        this.protocolID = protocolID + "";
    }

    public static class IPv4 extends IP
    {
        private static final String VERSION = "VER";
        private static final String HEADER_LENGTH = "H_LEN";
        private static final String TYPE_OF_SERVICE = "TOS";
        private static final String TOTAL_LENGTH = "TOT_LEN";
        private static final String IDENTIFICATION = "IDE";
        private static final String FLAGS = "FLAGS";
        private static final String FRAGMENT_OFFSET = "FRAG_OFF";
        private static final String TIME_TO_LIVE = "TTL";
        private static final String PROTOCOL = "PROT";
        private static final String HEADER_CHECKSUM = "H_CHECK";
        private static final String SOURCE_ADDRESS = "SOURCE";
        private static final String DESTINATION_ADDRESS = "DESTINATION";
        private static final String OPTIONS = "OPTS";
        private static final String PADDING = "PAD";
        
        public IPv4( final int protocolID ) {
            super( protocolID );
        }
        
        @Override
        public ProtocolEvent getEvent() {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public Packet makePacket()
        {
            // Size of the packet is [20 - 65.535].
            Packet packet = new Packet( 20, SizeUnit.BYTE );
            packet.addContent( VERSION, "" );
            packet.addContent( HEADER_LENGTH, "" );
            packet.addContent( TYPE_OF_SERVICE, "" );
            packet.addContent( TOTAL_LENGTH, "" );
            packet.addContent( IDENTIFICATION, "" );
            packet.addContent( FLAGS, "" );
            packet.addContent( FRAGMENT_OFFSET, "" );
            packet.addContent( TIME_TO_LIVE, "" );
            packet.addContent( PROTOCOL, protocolID );
            packet.addContent( HEADER_CHECKSUM, "" );
            packet.addContent( SOURCE_ADDRESS, source.getNode().getNetworkSettings().getIPv4address() );
            packet.addContent( DESTINATION_ADDRESS, dest.getNode().getNetworkSettings().getIPv4address() );
            packet.addContent( OPTIONS, "" );
            packet.addContent( PADDING, "" );
            return packet;
        }
    
        @Override
        public Event processPacket( final Packet packet )
        {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public String getID() {
            return "IPv4";
        }
    }
    
    public class IPv6 extends IP
    {
        private static final String VERSION = "VER";
        private static final String TRAFFIC_CLASS = "TRAFF_CLASS";
        private static final String FLOW_LABEL = "FLOW";
        private static final String PAYLOAD_LENGTH = "PAY_LEN";
        private static final String NEXT_HEADER = "NEXT_HEADER";
        private static final String HOP_LIMIT = "HOP";
        private static final String SOURCE_ADDRESS = "SOURCE";
        private static final String DESTINATION_ADDRESS = "DESTINATION";
        
        public IPv6( final int next_header ) {
            super( next_header );
        }
        
        @Override
        public ProtocolEvent getEvent() {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public Packet makePacket()
        {
            // Size of the packet is [20 - 65.535].
            Packet packet = new Packet( 20, SizeUnit.BYTE );
            packet.addContent( VERSION, "" );
            packet.addContent( TRAFFIC_CLASS, "" );
            packet.addContent( FLOW_LABEL, "" );
            packet.addContent( PAYLOAD_LENGTH, "" );
            packet.addContent( NEXT_HEADER, "" + protocolID );
            packet.addContent( HOP_LIMIT, "" );
            packet.addContent( SOURCE_ADDRESS, source.getNode().getNetworkSettings().getIPv6address() );
            packet.addContent( DESTINATION_ADDRESS, dest.getNode().getNetworkSettings().getIPv6address() );
            return packet;
        }
    
        @Override
        public Event processPacket( final Packet packet ) {
            // TODO Auto-generated method stub
            return null;
        }
        
        @Override
        public String getID() {
            return "IPv6";
        }
    }
}