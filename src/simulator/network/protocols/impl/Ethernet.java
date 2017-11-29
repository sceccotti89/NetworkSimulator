/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import simulator.network.ConnectionInfo;
import simulator.network.protocols.DataLinkProtocol;
import simulator.network.protocols.Header;
import simulator.network.protocols.Header.Range;
import simulator.network.protocols.ProtocolReference;

public class Ethernet extends DataLinkProtocol
{
    // First header.
    private static final Range PREAMBLE                 = new Range(   0, 56 );
    private static final Range START_OF_FRAME_DELIMETER = new Range(  56,  8 );
    private static final Range MAC_DESTINATION          = new Range(  64, 48 );
    private static final Range MAC_SOURCE               = new Range( 112, 48 );
    // TODO la posizione iniziale di ETHERTYPE dipende dalla presenza del campo 802.1Q tag (opzionale di 4 byte)
    private static final Range ETHERTYPE                = new Range( 160, 16 );
    
    // Second header.
    private static final Range FRAME_CHECK_SEQUENCE     = new Range(  0, 32 );
    private static final Range INTERPACKET_GAP          = new Range( 32, 96 );
    
    
    
    public Ethernet() {}
    
    public Ethernet( final int etherType ) {
        setEtherType( etherType );
    }
    
    private void setAddressValue( final Header header, final Range range, final String address )
    {
        int octet = 0;
        for (String value : address.split( "-" )) {
            int number = Integer.parseInt( value, 16 );
            header.setField( range.from() + octet, number, 8 );
            octet += 8;
        }
    }
    
    @Override
    public List<Header> makeHeader( final Header upperHeader, final ConnectionInfo info )
    {
        Header header = new Header( 128 );
        header.setField( FRAME_CHECK_SEQUENCE, 0 );
        header.setField( INTERPACKET_GAP, 0 );
        // Put the second header at the end of the input one.
        upperHeader.addHeader( header );
        
        header = new Header( ETHERTYPE.from() + ETHERTYPE.length() + upperHeader.getSizeInBits() );
        header.setField( PREAMBLE, 0 );
        header.setField( START_OF_FRAME_DELIMETER, 0 );
        setAddressValue( header, MAC_DESTINATION, info.getDestinationMACaddress() );
        setAddressValue( header, MAC_SOURCE, info.getSourceMACaddress() );
        header.setField( ETHERTYPE, etherType );
        header.addHeader( upperHeader );
        printHeader( header );
        return Collections.singletonList( header );
    }
    
    @Override
    public ProtocolReference processHeader( final Header header )
    {
        Header ethHeader = header.removeHeader( header.getSizeInBits() - 128, 128 );
        // TODO dovrebbe analizzarlo, ma lo ignoro
        
        ethHeader = header.removeHeader( ETHERTYPE.from() + ETHERTYPE.length() );
        return new ProtocolReference( ethHeader.getField( ETHERTYPE ) );
    }
    
    @Override
    public LinkedHashMap<String, String> getFields( final Header header )
    {
        LinkedHashMap<String,String> fields;
        if (header.getSizeInBytes() == 16) {
            fields = new LinkedHashMap<>( 2 );
            fields.put( "Frame Check Sequence", "" + header.getField( FRAME_CHECK_SEQUENCE ) );
            fields.put( "Interpacket Gap", "" + header.getField( INTERPACKET_GAP ) );
        } else {
            fields = new LinkedHashMap<>( 5 );
            fields.put( "Preamble", "" + header.getField( PREAMBLE ) );
            fields.put( "Start Frame Delimeter", "" + header.getField( START_OF_FRAME_DELIMETER ) );
            fields.put( "MAC Destination", "" + header.getField( MAC_DESTINATION ) );
            fields.put( "MAC Source", "" + header.getField( MAC_SOURCE ) );
            fields.put( "Ethertype", Integer.toHexString( header.getField( ETHERTYPE ) ) );
        }
        
        return fields;
    }
    
    private String getAddress( final Header header, final Range range )
    {
        String address = "";
        for (int i = 0; i < range.length() - 8; i += 8) {
            int value = header.getField( range.from() + i, 8 );
            String hex = Integer.toHexString( value );
            if (hex.length() == 1) hex = "0" + hex;
            address += hex + "-";
        }
        int value = header.getField( range.from() + 24, 8 );
        String hex = Integer.toHexString( value );
        if (hex.length() == 1) hex = "0" + hex;
        address += hex;
        
        return address.toUpperCase();
    }
    
    @Override
    public void printHeader( final Header header )
    {
        String content = "=============== ETHERNET ===============\n";
        if (header.getSizeInBytes() == 16) {
            content += "Frame Check Sequence: " + header.getField( FRAME_CHECK_SEQUENCE ) + "\n";
            content += "Interpacket Gap:      " + header.getField( INTERPACKET_GAP ) + "\n";
        } else {
            content += "Preamble:              " + header.getField( PREAMBLE ) + "\n";
            content += "Start Frame Delimeter: " + header.getField( START_OF_FRAME_DELIMETER ) + "\n";
            content += "MAC Destination:       " + getAddress( header, MAC_DESTINATION ) + "\n";
            content += "MAC Source:            " + getAddress( header, MAC_SOURCE ) + "\n";
            content += "EtherType:             0x" + Integer.toHexString( header.getField( ETHERTYPE ) ) + "\n";
        }
        content += "========================================\n";
        System.out.print( content );
    }
    
    @Override
    public String getName( final boolean extended ) {
        if (extended) return "Ethernet II";
        else return "ETH";
    }

    /*private static class Frame extends Header
    {
        public Frame( final int size ) {
            super( size );
        }
        
        @Override
        public LinkedHashMap<String, String> getFields()
        {
            LinkedHashMap<String,String> fields;
            if (getSizeInBytes() == 16) {
                fields = new LinkedHashMap<>( 2 );
                fields.put( "Frame Check Sequence", "" + getField( FRAME_CHECK_SEQUENCE ) );
                fields.put( "Interpacket Gap", "" + getField( INTERPACKET_GAP ) );
            } else {
                fields = new LinkedHashMap<>( 5 );
                fields.put( "Preamble", "" + getField( PREAMBLE ) );
                fields.put( "Start Frame Delimeter", "" + getField( START_OF_FRAME_DELIMETER ) );
                fields.put( "MAC Destination", "" + getField( MAC_DESTINATION ) );
                fields.put( "MAC Source", "" + getField( MAC_SOURCE ) );
                fields.put( "Ethertype", Integer.toHexString( getField( ETHERTYPE ) ) );
            }
            
            return fields;
        }
        
        private String getAddress( final Range range )
        {
            String address = "";
            for (int i = 0; i < range.length() - 8; i += 8) {
                int value = getField( range.from() + i, 8 );
                String hex = Integer.toHexString( value );
                if (hex.length() == 1) hex = "0" + hex;
                address += hex + "-";
            }
            int value = getField( range.from() + 24, 8 );
            String hex = Integer.toHexString( value );
            if (hex.length() == 1) hex = "0" + hex;
            address += hex;
            
            return address.toUpperCase();
        }
        
        @Override
        public String toString()
        {
            if (getNextHeader() != null) {
                System.out.print( getNextHeader() );
            }
            
            String content = "=============== ETHERNET ===============\n";
            if (getSizeInBytes() == 16) {
                content += "Frame Check Sequence: " + getField( FRAME_CHECK_SEQUENCE ) + "\n";
                content += "Interpacket Gap:      " + getField( INTERPACKET_GAP ) + "\n";
            } else {
                content += "Preamble:              " + getField( PREAMBLE ) + "\n";
                content += "Start Frame Delimeter: " + getField( START_OF_FRAME_DELIMETER ) + "\n";
                content += "MAC Destination:       " + getAddress( MAC_DESTINATION ) + "\n";
                content += "MAC Source:            " + getAddress( MAC_SOURCE ) + "\n";
                content += "EtherType:             0x" + Integer.toHexString( getField( ETHERTYPE ) ) + "\n";
            }
            content += "========================================\n";
            return content;
        }
    }*/
}