/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import simulator.network.ConnectionInfo;
import simulator.network.element.Router;
import simulator.network.protocols.Header;
import simulator.network.protocols.Header.Range;
import simulator.network.protocols.NetworkProtocol;
import simulator.network.protocols.ProtocolReference;
import simulator.network.protocols.TransportProtocol;

public abstract class IP extends NetworkProtocol
{
    protected int protocolID;
    protected String sourceAddress;
    protected String destinationAddress;
    
    private static final short NEXT_IDENTIFICATION = 0;
    
    protected static final Range VERSION = new Range( 0, 4 );
    
    
    public IP( int protocolID, int etherType )
    {
        this.protocolID = protocolID;
        // By default it makes use of Ethernet frames.
        setNextProtocol( new Ethernet( etherType ) );
    }
    
    public abstract int getMSS();
    
    private static synchronized short getIdentificationID() {
        return (NEXT_IDENTIFICATION + 1) % Short.MAX_VALUE;
    }

    public static class IPv4 extends IP
    {
        private static final Range INTERNET_HEADER_LENGTH    = new Range(   4,  4 );
        private static final Range DIFFERENTIATED_SERVICES   = new Range(   8,  8 );
        private static final Range TOTAL_LENGTH              = new Range(  16, 16 );
        private static final Range IDENTIFICATION            = new Range(  32, 16 );
        private static final Range FLAGS                     = new Range(  48,  3 );
        private static final Range FRAGMENT_OFFSET           = new Range(  51, 13 );
        private static final Range TIME_TO_LIVE              = new Range(  64,  8 );
        private static final Range PROTOCOL                  = new Range(  72,  8 );
        private static final Range HEADER_CHECKSUM           = new Range(  80, 16 );
        private static final Range SOURCE_ADDRESS            = new Range(  96, 32 );
        private static final Range DESTINATION_ADDRESS       = new Range( 128, 32 );
        //private static final Range OPTIONS                   = new Range( 160,  8 );
        //private static final Range PADDING                   = new Range( 168, 24 );
        
        private static final int SIZE = 20;
        
        private Map<Integer,List<Header>> fragments;
        
        
        
        public IPv4()
        {
            super( -1, -1 );
            fragments = new HashMap<>();
        }
        
        public IPv4( TransportProtocol protocol ) {
            this( protocol.getProtocol() );
        }
        
        public IPv4( int protocolID ) {
            super( protocolID, 0x0800 );
        }
        
        public void setOption() {
            // TODO andrebbe implementata
            
        }
        
        public void setFlag() {
            // TODO completare
        }
        
        private int findMaxPacketSize( int maxSize )
        {
            for (int i = 0; i < 8; i++) {
                if (maxSize % 8 == 0) {
                    return maxSize;
                } else {
                    maxSize--;
                }
            }
            return maxSize;
        }
        
        @Override
        public List<Header> makeHeader( Header upperHeader, ConnectionInfo info )
        {
            boolean isRouter = this.node instanceof Router;
            final int MTU = info.getLink().getMTU();
            final int maxSize = findMaxPacketSize( MTU - SIZE );
            int upHeaderSize = 0;
            if (upperHeader != null) {
                upHeaderSize = upperHeader.getSizeInBytes();
            }
            
            List<Header> headers = new ArrayList<>();
            if (/*TODO isRouter && */upHeaderSize > maxSize) {
                //System.out.println( "MULTI PACCHETTO: " + upHeaderSize + ", MAX: " + maxSize );
                final short identification = getIdentificationID();
                // Generate the first packet comprehending the transport header as payload (if any).
                Header header = createHeader( info, identification, 0, maxSize, 1 );
                header.addHeader( upperHeader.removeHeader( maxSize * Byte.SIZE ) );
                headers.add( header );
                
                //System.out.println( "LENGTH: " + maxSize );
                
                // Generate all the remaining packets without the transport header (if any).
                int byte_offset = maxSize;
                int totalSize = upHeaderSize - maxSize;
                for ( ; totalSize > maxSize; totalSize -= maxSize, byte_offset += maxSize) {
                    header = createHeader( info, identification, byte_offset, maxSize, 1 );
                    header.addHeader( upperHeader.removeHeader( maxSize * Byte.SIZE ) );
                    headers.add( header );
                    //System.out.println( "LENGTH: " + maxSize );
                }
                header = createHeader( info, identification, byte_offset, upHeaderSize - byte_offset, 0 );
                header.addHeader( upperHeader.removeHeader( (upHeaderSize - byte_offset) * Byte.SIZE ) );
                headers.add( header );
                //System.out.println( "LENGTH: " + (upHeaderSize - byte_offset) );
            } else {
                //System.out.println( "SINGOLO PACCHETTO: " + upHeaderSize + ", MAX: " + maxSize );
                // Complete IP packet: no fragmentation.
                Header header = createHeader( info, 0, 0, upHeaderSize, 0 );
                printHeader( header );
                header.addHeader( upperHeader );
                headers.add( header );
            }
            
            return headers;
        }
        
        private Header createHeader( ConnectionInfo info, int identification, int byte_offset, int length, int moreFragments )
        {
            // TODO IHL varia da 5 word (20 Byte) a 15 word (60 Byte) a seconda della presenza del campo opzioni.
            
            Header header = new Header( SIZE * Byte.SIZE );
            header.setField( VERSION, 4 );
            header.setField( INTERNET_HEADER_LENGTH, SIZE / 4 );
            header.setField( DIFFERENTIATED_SERVICES, new int[]{0,0,1,0,1,0,0,0} ); // TODO Scelti a caso, solo per metterci qualcosa
            header.setField( TOTAL_LENGTH, SIZE + length );
            header.setField( IDENTIFICATION, identification );
            header.setField( FLAGS, new int[]{0,0,moreFragments} ); // TODO Primi 2 bit messi a caso
            header.setField( FRAGMENT_OFFSET, byte_offset / 8 );
            header.setField( TIME_TO_LIVE, 64 );
            header.setField( PROTOCOL, protocolID );
            header.setField( HEADER_CHECKSUM, 0x0FFF );
            setAddressValue( header, SOURCE_ADDRESS, node.getNetSettings().getIPv4address() );
            setAddressValue( header, DESTINATION_ADDRESS, info.getDestinationIPaddress() );
            //header.setField( OPTIONS, 0 );                          // Options
            // TODO il padding va aggiunto nel caso ci siano delle opzioni, in modo da farlo stare in 32 bits
            //header.setField( PADDING, 0 );                          // Padding
            
            return header;
        }
        
        private void setAddressValue( Header header, Range range, String address )
        {
            int octet = 0;
            for (String value : address.split( "\\." )) {
                int number = Integer.parseInt( value );
                header.setField( range.from() + octet, number, 8 );
                octet += 8;
            }
        }
        
        private int getPayloadSize( Header header ) {
            return header.getField( TOTAL_LENGTH ) - header.getField( INTERNET_HEADER_LENGTH ) * 4;
        }
        
        @Override
        public ProtocolReference processHeader( Header header )
        {
            System.out.println( "IP HEADER: " );
            printHeader( header.getHeader( header.getField( INTERNET_HEADER_LENGTH ) * 4 * Byte.SIZE ) );
            
            int offset = header.getField( FRAGMENT_OFFSET ) * 8;
            boolean fragmentation = header.getBitField( FLAGS, 2 ) == 1; // More fragments.
            if (fragmentation || offset > 0) {
                int index = 0;
                int id = header.getField( IDENTIFICATION );
                List<Header> frags = fragments.get( id );
                
                boolean completed = true;
                if (frags == null) {
                    frags = new ArrayList<>();
                    fragments.put( id, frags );
                }
                
                // Put the fragment in the correct position.
                int currentOffset = 0;
                for (Header h : frags) {
                    int frag_off = h.getField( FRAGMENT_OFFSET ) * 8;
                    //System.out.println( "CURRENT: " + currentOffset + ", FRAG: " + frag_off );
                    if (offset > frag_off) {
                        index++;
                    }
                    if (currentOffset == frag_off) {
                        currentOffset += getPayloadSize( h );
                    } else {
                        completed = false;
                    }
                }
                frags.add( index, header );
                
                completed &= !fragmentation;
                //System.out.println( "COMPLETED: " + completed + ", FRAG: " + fragmentation + ", OFFSET: " + offset );
                
                if (!completed) {
                    return null;
                }
                
                fragments.remove( id );
                int protocol = frags.get( 0 ).getField( PROTOCOL );
                
                // Reconstruct the packet.
                Header payload = new Header( currentOffset );
                for (Header h : frags) {
                    h.removeHeader( h.getField( INTERNET_HEADER_LENGTH ) * 4 * Byte.SIZE );
                    payload.addHeader( h );
                }
                
                header.clear( currentOffset );
                header.addHeader( payload );
                
                return new ProtocolReference( protocol );
            }
            
            Header ipHeader = header.removeHeader( header.getField( INTERNET_HEADER_LENGTH ) * 4 * Byte.SIZE );
            return new ProtocolReference( ipHeader.getField( PROTOCOL ) );
        }
        
        private String getAddress( Header header, Range range )
        {
            String address = "";
            for (int i = 0; i < range.length() - 8; i += 8) {
                int value = header.getField( range.from() + i, 8 );
                address += value + ".";
            }
            int value = header.getField( range.from() + 24, 8 );
            address += value;
            
            return address;
        }
        
        @Override
        public LinkedHashMap<String, String> getFields( Header header )
        {
            LinkedHashMap<String,String> fields = new LinkedHashMap<>( 14 );
            fields.put( "Version", "" + header.getField( VERSION ) );
            fields.put( "Internet Header Length", "" + header.getField( INTERNET_HEADER_LENGTH ) );
            fields.put( "Differentiated Services", "" + getBitField( header, DIFFERENTIATED_SERVICES ) );
            fields.put( "Total Length", "" + header.getField( TOTAL_LENGTH ) );
            fields.put( "Identification", "" + header.getField( IDENTIFICATION ) );
            fields.put( "Flags", "" + getBitField( header, FLAGS ) );
            fields.put( "Fragment Offset", "" + header.getField( FRAGMENT_OFFSET ) );
            fields.put( "Time To Live", "" + header.getField( TIME_TO_LIVE ) );
            fields.put( "Protocol", "" + header.getField( PROTOCOL ) );
            fields.put( "Header Checksum", "" + header.getField( HEADER_CHECKSUM ) );
            fields.put( "Source Address", "" + getAddress( header, SOURCE_ADDRESS ) );
            fields.put( "Destination Address", "" + getAddress( header, DESTINATION_ADDRESS ) );
            return fields;
        }
        
        @Override
        public void printHeader( Header header )
        {
            System.out.println( "================= IPv4 =================" );
            System.out.println( "Version:                 " + header.getField( VERSION ) );
            System.out.println( "Internet Header Length:  " + header.getField( INTERNET_HEADER_LENGTH ) );
            System.out.println( "Differentiated Services: " + getBitField( header, DIFFERENTIATED_SERVICES ) );
            System.out.println( "Total Length:            " + header.getField( TOTAL_LENGTH ) );
            System.out.println( "Identification:          " + header.getField( IDENTIFICATION ) );
            System.out.println( "Flags:                   " + getBitField( header, FLAGS ) );
            System.out.println( "Fragment Offset:         " + header.getField( FRAGMENT_OFFSET ) );
            System.out.println( "Time To Live:            " + header.getField( TIME_TO_LIVE ) );
            System.out.println( "Protocol:                " + header.getField( PROTOCOL ) );
            System.out.println( "Header Checksum:         " + header.getField( HEADER_CHECKSUM ) );
            System.out.println( "Source Address:          " + getAddress( header, SOURCE_ADDRESS ) );
            System.out.println( "Destination Address:     " + getAddress( header, DESTINATION_ADDRESS ) );
            System.out.println( "========================================" );
        }
        
        @Override
        public int getEtherType() {
            return 0x0800;
        }
        
        
        /*private static class Datagram extends Header
        {
            public Datagram( int size ) {
                super( size );
            }
            
            private String getAddress( Range range )
            {
                String address = "";
                for (int i = 0; i < range.length() - 8; i += 8) {
                    int value = getField( range.from() + i, 8 );
                    address += value + ".";
                }
                int value = getField( range.from() + 24, 8 );
                address += value;
                
                return address;
            }
            
            private String getBitField( Range range )
            {
                String value = "";
                for (int i = 0; i < range.length(); i++) {
                    value += getField( range.from() + i, 1 );
                }
                return value;
            }
            
            @Override
            public LinkedHashMap<String, String> getFields()
            {
                LinkedHashMap<String,String> fields = new LinkedHashMap<>( 14 );
                fields.put( "Version", "" + getField( VERSION ) );
                fields.put( "Internet Header Length", "" + getField( INTERNET_HEADER_LENGTH ) );
                fields.put( "Differentiated Services", "" + getBitField( DIFFERENTIATED_SERVICES ) );
                fields.put( "Total Length", "" + getField( TOTAL_LENGTH ) );
                fields.put( "Identification", "" + getField( IDENTIFICATION ) );
                fields.put( "Flags", "" + getBitField( FLAGS ) );
                fields.put( "Fragment Offset", "" + getField( FRAGMENT_OFFSET ) );
                fields.put( "Time To Live", "" + getField( TIME_TO_LIVE ) );
                fields.put( "Protocol", "" + getField( PROTOCOL ) );
                fields.put( "Header Checksum", "" + getField( HEADER_CHECKSUM ) );
                fields.put( "Source Address", "" + getAddress( SOURCE_ADDRESS ) );
                fields.put( "Destination Address", "" + getAddress( DESTINATION_ADDRESS ) );
                return fields;
            }
            
            @Override
            public String toString()
            {
                if (getNextHeader() != null) {
                    System.out.print( getNextHeader() );
                }
                
                String content = "================= IPv4 =================\n";
                content += "Version:                 " + getField( VERSION ) + "\n";
                content += "Internet Header Length:  " + getField( INTERNET_HEADER_LENGTH ) + "\n";
                content += "Differentiated Services: " + getBitField( DIFFERENTIATED_SERVICES ) + "\n";
                content += "Total Length:            " + getField( TOTAL_LENGTH ) + "\n";
                content += "Identification:          " + getField( IDENTIFICATION ) + "\n";
                content += "Flags:                   " + getBitField( FLAGS ) + "\n";
                content += "Fragment Offset:         " + getField( FRAGMENT_OFFSET ) + "\n";
                content += "Time To Live:            " + getField( TIME_TO_LIVE ) + "\n";
                content += "Protocol:                " + getField( PROTOCOL ) + "\n";
                content += "Header Checksum:         " + getField( HEADER_CHECKSUM ) + "\n";
                content += "Source Address:          " + getAddress( SOURCE_ADDRESS ) + "\n";
                content += "Destination Address:     " + getAddress( DESTINATION_ADDRESS ) + "\n";
                content += "========================================\n";
                return content;
            }
        }*/
        
        @Override
        public int getMSS() {
            return 576 - SIZE;
        }
        
        @Override
        public String getName( boolean extended ) {
            if (extended) return "Internet Protocol version 4";
            else return "IPv4";
        }
    }
    
    public static class IPv6 extends IP
    {
        private static final Range TRAFFIC_CLASS         = new Range(   4,   8 );
        private static final Range FLOW_LABEL            = new Range(  12,  20 );
        private static final Range PAYLOAD_LENGTH        = new Range(  32,  16 );
        private static final Range NEXT_HEADER           = new Range(  48,   8 );
        private static final Range HOP_LIMIT             = new Range(  56,   8 );
        private static final Range SOURCE_ADDRESS        = new Range(  64, 128 );
        private static final Range DESTINATION_ADDRESS   = new Range( 192, 128 );
        
        private static final int SIZE = 40;
        
        public IPv6() {
            super( -1, -1 );
        }
        
        public IPv6( int next_header ) {
            super( next_header, 0x86DD );
        }
        
        @Override
        public List<Header> makeHeader( Header upperHeader, ConnectionInfo info )
        {
            final int identification = getIdentificationID();
            Header header = new Header( SIZE * Byte.SIZE );
            header.setField( VERSION, 4 );
            header.setField( TRAFFIC_CLASS, -1 );
            header.setField( FLOW_LABEL, identification );
            header.setField( PAYLOAD_LENGTH, upperHeader.getSizeInBytes() );
            header.setField( NEXT_HEADER, protocolID );
            header.setField( HOP_LIMIT, 64 );
            setAddressValue( header, SOURCE_ADDRESS, node.getNetSettings().getIPv6address() );
            setAddressValue( header, DESTINATION_ADDRESS, info.getDestinationIPaddress() );
            return Collections.singletonList( header );
        }
        
        private void setAddressValue( Header header, Range range, String address )
        {
            int hextet = 0;
            for (String value : address.split( ":" )) {
                if (value.isEmpty()) {
                    final int blocks = 8 - value.length();
                    for (int i = 0; i < blocks; i++) {
                        header.setField( range.from() + hextet, 0, 16 );
                        hextet += 16;
                    }
                } else {
                    int number = Integer.parseInt( value );
                    header.setField( range.from() + hextet, number, 16 );
                    hextet += 16;
                }
            }
        }
        
        @Override
        public ProtocolReference processHeader( Header header ) {
            return new ProtocolReference( header.getField( NEXT_HEADER ) );
        }
        
        private String getAddress( Header header, Range range )
        {
            String address = "";
            for (int i = 0; i < range.length() - 16; i += 16) {
                int value = header.getField( range.from() + i, 16 );
                address += value + ":";
            }
            int value = header.getField( range.from() + 112, 16 );
            address += value;
            
            return address;
        }
        
        @Override
        public LinkedHashMap<String, String> getFields( Header header )
        {
            LinkedHashMap<String,String> fields = new LinkedHashMap<>( 8 );
            fields.put( "Version", "" + header.getField( VERSION ) );
            fields.put( "Traffic Class", "" + header.getField( TRAFFIC_CLASS ) );
            fields.put( "Flow Label", "" + header.getField( FLOW_LABEL ) );
            fields.put( "Payload Length", "" + header.getField( PAYLOAD_LENGTH ) );
            fields.put( "Next Header", "" + header.getField( NEXT_HEADER ) );
            fields.put( "Hop Limit", "" + header.getField( HOP_LIMIT ) );
            fields.put( "Source Address", "" + getAddress( header, SOURCE_ADDRESS ) );
            fields.put( "Destination Address", "" + getAddress( header, DESTINATION_ADDRESS ) );
            return fields;
        }
        
        @Override
        public void printHeader( Header header )
        {
            String content = "================= IPv6 =================\n";
            content += "Version:             " + header.getField( VERSION ) + "\n";
            content += "Traffic Class:       " + header.getField( TRAFFIC_CLASS ) + "\n";
            content += "Flow Label:          " + header.getField( FLOW_LABEL ) + "\n";
            content += "Payload Length:      " + header.getField( PAYLOAD_LENGTH ) + "\n";
            content += "Next Header:         " + header.getField( NEXT_HEADER ) + "\n";
            content += "Hop Limit:           " + header.getField( HOP_LIMIT ) + "\n";
            content += "Source Address:      " + getAddress( header, SOURCE_ADDRESS ) + "\n";
            content += "Destination Address: " + getAddress( header, DESTINATION_ADDRESS ) + "\n";
            content += "========================================\n";
            System.out.print( content );
        }
        
        @Override
        public int getEtherType() {
            return 0x86DD;
        }
        
        /*private static class Datagram extends Header
        {
            public Datagram( int size ) {
                super( size );
            }
            
            private String getAddress( Range range )
            {
                String address = "";
                for (int i = 0; i < range.length() - 16; i += 16) {
                    int value = getField( range.from() + i, 16 );
                    address += value + ":";
                }
                int value = getField( range.from() + 112, 16 );
                address += value;
                
                return address;
            }
            
            @Override
            public LinkedHashMap<String, String> getFields()
            {
                LinkedHashMap<String,String> fields = new LinkedHashMap<>( 8 );
                fields.put( "Version", "" + getField( VERSION ) );
                fields.put( "Traffic Class", "" + getField( TRAFFIC_CLASS ) );
                fields.put( "Flow Label", "" + getField( FLOW_LABEL ) );
                fields.put( "Payload Length", "" + getField( PAYLOAD_LENGTH ) );
                fields.put( "Next Header", "" + getField( NEXT_HEADER ) );
                fields.put( "Hop Limit", "" + getField( HOP_LIMIT ) );
                fields.put( "Source Address", "" + getAddress( SOURCE_ADDRESS ) );
                fields.put( "Destination Address", "" + getAddress( DESTINATION_ADDRESS ) );
                return fields;
            }
            
            @Override
            public String toString()
            {
                if (getNextHeader() != null) {
                    System.out.print( getNextHeader() );
                }
                
                String content = "================= IPv6 =================\n";
                content += "Version:             " + getField( VERSION ) + "\n";
                content += "Traffic Class:       " + getField( TRAFFIC_CLASS ) + "\n";
                content += "Flow Label:          " + getField( FLOW_LABEL ) + "\n";
                content += "Payload Length:      " + getField( PAYLOAD_LENGTH ) + "\n";
                content += "Next Header:         " + getField( NEXT_HEADER ) + "\n";
                content += "Hop Limit:           " + getField( HOP_LIMIT ) + "\n";
                content += "Source Address:      " + getAddress( SOURCE_ADDRESS ) + "\n";
                content += "Destination Address: " + getAddress( DESTINATION_ADDRESS ) + "\n";
                content += "========================================\n";
                return content;
            }
        }*/
        
        @Override
        public int getMSS() {
            return 1280 - SIZE;
        }
        
        @Override
        public String getName( boolean extended ) {
            if (extended) return "Internet Protocol version 6";
            else return "IPv6";
        }
    }
}
