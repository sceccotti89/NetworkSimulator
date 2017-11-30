/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import simulator.core.Agent;
import simulator.network.ConnectionInfo;
import simulator.network.NetworkLayer;
import simulator.network.protocols.EventProtocol;
import simulator.network.protocols.Header;
import simulator.network.protocols.Header.Range;
import simulator.network.protocols.Protocol;
import simulator.network.protocols.ProtocolReference;
import simulator.network.protocols.TransportProtocol;
import simulator.network.protocols.impl.IP.IPv4;

public class TCP extends TransportProtocol implements EventProtocol
{
    private static final Range SOURCE_PORT            = new Range(   0, 16 );
    private static final Range DESTINATION_PORT       = new Range(  16, 16 );
    private static final Range SEQUENCE_NUMBER        = new Range(  32, 32 );
    private static final Range ACKNOWLEDGEMENT_NUMBER = new Range(  64, 32 );
    private static final Range DATA_OFFSET            = new Range(  96,  4 );
    private static final Range RESERVED               = new Range( 100,  3 );
    private static final Range ECN                    = new Range( 103,  3 );
    private static final Range CONTROL_BITS           = new Range( 106,  6 );
    private static final Range WINDOW                 = new Range( 112, 16 );
    private static final Range CHECKSUM               = new Range( 128, 16 );
    private static final Range URGENT_POINTER         = new Range( 144, 16 );
    //private static final Range OPTIONS = new Range( 160, 0-40 );
    
    private static final int URG = 0;
    private static final int ACK = 1;
    private static final int PSH = 2;
    private static final int RST = 3;
    private static final int SYN = 4;
    private static final int FIN = 5;
    
    private static final int SIZE = 20;
    
    private static final int CLOSED       =  0;
    private static final int LISTEN       =  1;
    private static final int SYN_RECEIVED =  2;
    private static final int SYN_SENT     =  3;
    private static final int ESTABLISHED  =  4;
    private static final int FIN_WAIT_1   =  5;
    private static final int FIN_WAIT_2   =  6;
    private static final int CLOSING      =  7;
    private static final int TIME_WAIT    =  8;
    private static final int CLOSE_WAIT   =  9;
    private static final int LAST_ACK     = 10;
    
    private int sourcePort;
    private int destPort;
    
    private int MSS;
    private int offset = 0;
    
    private int state = CLOSED;
    
    private int bufferSize;
    
    private boolean complete = false;
    
    private byte[] message;
    
    private final Random random = new Random();
    private int sequenceNumber;
    private int acknowledgement = 0;
    
    // TODO utilizzare un oggetto che verra utilizzato per eseguire correttamente la trasmissione dei messaggi
    // TODO probabilmente sara' una lista, ma in ogni caso valutare al momento.
    // TODO molto probabilmente potrebbe bastare una semplice variabile che indichi l'ultimo byte ricevuto e l'ultimo inviato.
    //List<> packets;
    
    
    
    public TCP( Protocol... baseProtocols ) {
        super( NetworkLayer.TRANSPORT, baseProtocols );
    }
    
    public TCP( int port )
    {
        super( NetworkLayer.TRANSPORT );
        
        this.sourcePort = port;
        this.destPort = port;
        // By default it makes use of IPv4.
        IP IpProtocol = new IPv4( getProtocol() );
        setNextProtocol( IpProtocol );
        
        MSS = IpProtocol.getMSS() - SIZE;
        
        getSequenceNumber();
    }
    
    @Override
    public void setAgent( Agent node )
    {
        node.removeAvailablePort( sourcePort );
        bufferSize = node.getBufferSize();
        super.setAgent( node );
    }
    
    // TODO in pratica MSS andra' utilizzato come MTU per il livello IP, cioe' spedire parti di messaggio in base a tale valore.
    // TODO per realizzare CWIN e WIN ci sara' da divertirsi con tutti quei valori..
    
    @Override
    public List<Header> makeHeader( Header upperHeader, ConnectionInfo info )
    {
        if (state <= 3) {
            return handShake( upperHeader, info );
        } else if (state == ESTABLISHED) {
            if (message == null) {
                message = upperHeader.getBytes();
            }
            return established( upperHeader, info );
        } else {
            return close( upperHeader, info );
        }
    }
    
    private List<Header> handShake( Header upperHeader, ConnectionInfo info )
    {
        Header header = null;
        switch (state) {
            case( CLOSED ):
                state = SYN_SENT;
                header = createHeader( info, null, SYN );
                /*TODO inizia la fase di hand-shake*/
                break;
        }
        
        return Collections.singletonList( header );
    }
    
    private List<Header> established( Header upperHeader, ConnectionInfo info )
    {
        // TODO terminato di spedire tutto il messaggio metterlo di nuovo a NULL.
        // TODO creare tanti messaggi quanto e' l'MSS
        
        createHeader( info, upperHeader, ACK );
        return null;
    }
    
    private List<Header> close( Header upperHeader, ConnectionInfo info )
    {
        return null;
    }
    
    @Override
    public ProtocolReference processHeader( Header header )
    {
        Header tcpHeader = header.removeHeader( SIZE * Byte.SIZE );
        Header response = null;
        
        // TODO Completare
        switch (state) {
            case( CLOSED ):
                state = LISTEN;
                if (checkBits( tcpHeader, SYN )) {
                    acknowledgement = tcpHeader.getField( SEQUENCE_NUMBER ) + 1;
                    response = createHeader( tcpHeader, null, SYN, ACK );
                }
                break;
            
            case( LISTEN ):
                if (!checkBits( tcpHeader, ACK )) {
                    // Ignore the message.
                    return null;
                }
                state = ESTABLISHED;
                complete = true;
                break;
            
            case( SYN_SENT ):
                if (checkBits( tcpHeader, SYN )) {
                    response = createHeader( tcpHeader, null, SYN, ACK );
                }
                if (checkBits( tcpHeader, SYN, ACK )) {
                    sequenceNumber += 1;
                    acknowledgement = tcpHeader.getField( SEQUENCE_NUMBER ) + 1;
                    response = createHeader( tcpHeader, null, ACK );
                }
                break;
            
            case( ESTABLISHED ):
                if (checkBits( tcpHeader, ACK )) {
                    acknowledgement += header.getSizeInBytes();
                    response = createHeader( tcpHeader, null, ACK );
                }
                // TODO Completare.
                break;
            case( CLOSE_WAIT ):
                // TODO Completare.
                break;
            case( LAST_ACK ):
                // TODO Completare.
                break;
        }
        
        return new ProtocolReference( response );
    }
    
    private boolean checkBits( Header h, Integer... controlBits )
    {
        for (Integer bit : controlBits) {
            if (h.getBitField( CONTROL_BITS.from() + bit ) != 1) {
                return false;
            }
        }
        return true;
    }
    
    private Header createHeader( Header inputHeader, Header payload, Integer... controlBits )
    {
        final int sourcePort = inputHeader.getField( DESTINATION_PORT );
        final int destPort   = inputHeader.getField( SOURCE_PORT );
        return createHeader( sourcePort, destPort, payload, controlBits );
    }
    
    private Header createHeader( ConnectionInfo info, Header payload, Integer... controlBits )
    {
        final int sourcePort = info.getDestinationPort();
        final int destPort   = info.getSourcePort();
        return createHeader( sourcePort, destPort, payload, controlBits );
    }
    
    private Header createHeader( int sourcePort, int destPort, Header payload, Integer... controlBits )
    {
        int payloadSize = (payload == null) ? 0 : payload.getSizeInBits();
        Header header = new Header( SIZE * Byte.SIZE + payloadSize );
        header.setField( SOURCE_PORT, sourcePort );
        header.setField( DESTINATION_PORT, destPort );
        header.setField( SEQUENCE_NUMBER, sequenceNumber );
        header.setField( ACKNOWLEDGEMENT_NUMBER, acknowledgement );
        
        // TODO completare con i valori delle finestre
        
        for (Integer bit : controlBits) {
            header.setField( CONTROL_BITS.from() + bit, 1, 1 );
        }
        
        if (payload != null) {
            header.addHeader( payload );
        }
        
        return header;
    }
    
    @Override
    public Header processEvent( TimeoutEvent event )
    {
        // TODO Leggendo pero' si capisce che il rinvio non e' di ogni singolo pacchetto ma solo dell'ultimo.
        // TODO Recuperare il messaggio associato all'event.
        
        return null;
    }

    @Override
    public boolean connectionDone() {
        return complete;
    }

    @Override
    public int getProtocol() {
        return 6;
    }

    @Override
    public String getIdentifier() {
        return sourcePort + "";
    }

    @Override
    public void printHeader( Header header )
    {
        String content = "================== UDP =================\n";
        content += "Source Port:            " + header.getField( SOURCE_PORT ) + "\n";
        content += "Destination Port:       " + header.getField( DESTINATION_PORT ) + "\n";
        content += "Sequence Number:        " + header.getField( SEQUENCE_NUMBER ) + "\n";
        content += "Acknowledgement Number: " + header.getField( ACKNOWLEDGEMENT_NUMBER ) + "\n";
        content += "Data Offset:            " + header.getField( DATA_OFFSET ) + "\n";
        content += "Reserved:               " + header.getField( RESERVED ) + "\n";
        content += "ECN:                    " + getBitField( header, ECN ) + "\n";
        content += "Control Bits:           " + getBitField( header, CONTROL_BITS ) + "\n";
        content += "Window:                 " + header.getField( WINDOW ) + "\n";
        content += "Checksum:               " + header.getField( CHECKSUM ) + "\n";
        content += "Urgent Pointer:         " + header.getField( URGENT_POINTER ) + "\n";
        content += "========================================\n";
        System.out.print( content );
    }
    
    @Override
    public LinkedHashMap<String, String> getFields( Header header )
    {
        LinkedHashMap<String,String> fields = new LinkedHashMap<>( 11 );
        fields.put( "Source Port", "" + header.getField( SOURCE_PORT ) );
        fields.put( "Destination Port", "" + header.getField( DESTINATION_PORT ) );
        fields.put( "Sequence Number", "" + header.getField( SEQUENCE_NUMBER ) );
        fields.put( "Acknowledgement Number", "" + header.getField( ACKNOWLEDGEMENT_NUMBER ) );
        fields.put( "Data Offset", "" + header.getField( DATA_OFFSET ) );
        fields.put( "Reserved", "" + header.getField( RESERVED ) );
        fields.put( "ECN", "" + getBitField( header, ECN ) );
        fields.put( "Control Bits", "" + getBitField( header, CONTROL_BITS ) );
        fields.put( "Window", "" + header.getField( WINDOW ) );
        fields.put( "Checksum", "" + header.getField( CHECKSUM ) );
        fields.put( "Urgent Pointer", "" + header.getField( URGENT_POINTER ) );
        return fields;
    }
    
    @Override
    public String getName( boolean extended ) {
        if (extended) return "Transmission Control Protocol";
        else return "TCP";
    }
    
    private void getSequenceNumber() {
        sequenceNumber = random.nextInt();
    }
}
