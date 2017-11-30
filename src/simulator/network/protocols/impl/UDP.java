/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import simulator.core.Agent;
import simulator.network.ConnectionInfo;
import simulator.network.NetworkLayer;
import simulator.network.protocols.Header;
import simulator.network.protocols.Header.Range;
import simulator.network.protocols.ProtocolReference;
import simulator.network.protocols.TransportProtocol;
import simulator.network.protocols.impl.IP.IPv4;

public class UDP extends TransportProtocol
{
    private static final Range SOURCE_PORT      = new Range(  0, 16 );
    private static final Range DESTINATION_PORT = new Range( 16, 16 );
    private static final Range LENGTH           = new Range( 32, 16 );
    private static final Range CHECKSUM         = new Range( 48, 16 );
    
    private static final int SIZE = 8;
    
    private int sourcePort;
    private int destPort;
    
    
    public UDP() {
        super( NetworkLayer.TRANSPORT );
    }
    
    public UDP( int port )
    {
        super( NetworkLayer.TRANSPORT );
        
        this.sourcePort = port;
        this.destPort = port;
        // By default it makes use of IPv4.
        setNextProtocol( new IPv4( getProtocol() ) );
    }
    
    @Override
    public void setAgent( Agent node )
    {
        node.removeAvailablePort( sourcePort );
        super.setAgent( node );
    }

    @Override
    public List<Header> makeHeader( Header upperHeader, ConnectionInfo info )
    {
        Header header = new Header( SIZE * Byte.SIZE + upperHeader.getSizeInBits() );
        header.setField( SOURCE_PORT, info.getSourcePort() );
        header.setField( DESTINATION_PORT, destPort );
        header.setField( LENGTH, 8 + upperHeader.getSizeInBytes() );
        header.setField( CHECKSUM, 0 );
        header.addHeader( upperHeader );
        printHeader( header );
        return Collections.singletonList( header );
    }
    
    @Override
    public ProtocolReference processHeader( Header header )
    {
        Header udpHeader = header.removeHeader( SIZE * Byte.SIZE );
        System.out.println( "RIMOSSO: " );
        printHeader( udpHeader );
        return new ProtocolReference( udpHeader.getField( DESTINATION_PORT ) );
    }
    
    @Override
    public boolean connectionDone() {
        return true;
    }

    @Override
    public int getProtocol() {
        return 17;
    }

    @Override
    public String getIdentifier() {
        return sourcePort + "";
    }

    @Override
    public void printHeader( Header header )
    {
        String content = "================== UDP =================\n";
        content += "Source Port:      " + header.getField( SOURCE_PORT ) + "\n";
        content += "Destination Port: " + header.getField( DESTINATION_PORT ) + "\n";
        content += "Length:           " + header.getField( LENGTH ) + "\n";
        content += "Checksum:         " + header.getField( CHECKSUM ) + "\n";
        content += "========================================\n";
        System.out.print( content );
    }
    
    @Override
    public LinkedHashMap<String, String> getFields( Header header )
    {
        LinkedHashMap<String,String> fields = new LinkedHashMap<>( 4 );
        fields.put( "Source Port", "" + header.getField( SOURCE_PORT ) );
        fields.put( "Destination Port", "" + header.getField( DESTINATION_PORT ) );
        fields.put( "Length", "" + header.getField( LENGTH ) );
        fields.put( "Checksum", "" + header.getField( CHECKSUM ) );
        return fields;
    }
    
    @Override
    public String getName( boolean extended ) {
        if (extended) return "UserDatagram Protocol";
        else return "UDP";
    }

    /*private static class Datagram extends Header
    {
        public Datagram( int size ) {
            super( size );
        }
        
        @Override
        public LinkedHashMap<String, String> getFields()
        {
            LinkedHashMap<String,String> fields = new LinkedHashMap<>( 4 );
            fields.put( "Source Port", "" + getField( SOURCE_PORT ) );
            fields.put( "Destination Port", "" + getField( DESTINATION_PORT ) );
            fields.put( "Length", "" + getField( LENGTH ) );
            fields.put( "Checksum", "" + getField( CHECKSUM ) );
            return fields;
        }
        
        @Override
        public String toString()
        {
            if (getNextHeader() != null) {
                System.out.print( getNextHeader() );
            }
            
            String content = "================== UDP =================\n";
            content += "Source Port:      " + getField( SOURCE_PORT ) + "\n";
            content += "Destination Port: " + getField( DESTINATION_PORT ) + "\n";
            content += "Length:           " + getField( LENGTH ) + "\n";
            content += "Checksum:         " + getField( CHECKSUM ) + "\n";
            content += "========================================\n";
            return content;
        }
    }*/
}
