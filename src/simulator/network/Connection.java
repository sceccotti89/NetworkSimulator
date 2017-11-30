/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.network.protocols.Header;
import simulator.network.protocols.Protocol;
import simulator.network.protocols.TransportProtocol;
import simulator.topology.NetworkLink;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public class Connection implements Closeable
{
    private Agent source;
    private Agent dest;
    
    private NetworkTopology net;
    private Map<Integer,Protocol> protocols;
    private int layer;
    private ConnectionInfo info;
    
    private int bufferSpace;
    
    
    
    public Connection( Agent source, NetworkTopology net, TransportProtocol protocol ) {
        this( source, null, net, protocol );
    }
    
    public Connection( Agent source, String destination, NetworkTopology net, TransportProtocol protocol )
    {
        this.source = source;
        this.net = net;
        protocols = new LinkedHashMap<>( NetworkLayer.STACK_LENGTH );
        protocols.put( protocol.getLayer().getIndex(), protocol );
        layer = protocol.getLayer().getIndex();
        protocol.setAgent( source );
        Protocol net_prot = protocol;
        while ((net_prot = net_prot.getNextProtocol()) != null) {
            protocols.put( net_prot.getLayer().getIndex(), net_prot );
            net_prot.setAgent( source );
        }
        
        for (int layer = 1; layer < NetworkLayer.STACK_LENGTH; layer++) {
            List<Protocol> netProtocols = source.getNetSettings().getNetworkProtocols( layer );
            if (netProtocols != null) {
                for (Protocol prot : netProtocols) {
                    if (!protocols.containsKey( layer )) {
                        protocols.put( layer, prot );
                        // Assign this protocol as the next one.
                        protocols.get( layer+1 ).setNextProtocol( prot );
                    }
                }
            }
        }
        
        if (destination != null) {
            // TODO dest = net.getAgent( destination );
        }
        info = new ConnectionInfo();
        info.setDestinationIPaddress( destination );
    }
    
    public Protocol getProtocol( NetworkLayer layer ) {
        return protocols.get( layer.getIndex() );
    }
    
    protected void setBufferSpace( int size ) {
        bufferSpace = size;
    }
    
    protected boolean checkBufferSpace( int size )
    {
        if (bufferSpace >= size) {
            bufferSpace -= size;
            return true;
        } else {
            return false;
        }
    }
    
    protected void freeBufferSpace( int size ) {
        bufferSpace += size;
    }
    
    // TODO Per il MAC address dovrebbe consultare il protocollo ARP!!!
    
    public void sendMessage( String destination, Message message )
    {
        if (dest != null) {
            throw new RuntimeException( "Destination is already specified." );
        }
        
        // TODO dest = net.getAgent( destination );
        info.setDestinationIPaddress( destination );
        sendMessage( message );
    }
    
    public void sendMessage( Message message )
    {
        if (dest == null) {
            throw new RuntimeException( "Destination not specified." );
        }
        
        // TODO Implementare
        /*if (!protocol.connectionDone()) {
            // TODO si salva il messaggio da spedire.
            return;
        }*/
        
        long nextNode = net.nextNode( source.getId(), dest.getId() ).getId();
        NetworkLink link = net.getLink( source.getId(), nextNode );
        
        info.setSourceMACaddress( source.getNetSettings().getMACaddress() );
        info.setDestinationMACaddress( dest.getNetSettings().getMACaddress() );
        info.setSourcePort( source.getAvailablePort() );
        info.setLink( link );
        
        if (message != null) {
            send( layer, new Header( message.getBytes() ) );
        } else {
            send( layer, null );
        }
    }
    
    protected long send( int layer, Header header )
    {
        long expiredTime = 0;
        Protocol protocol = protocols.get( layer );
        System.out.println( "LIVELLO: " + layer + ", PROTOCOLLO: " + protocol );
        if (protocol != null) {
            List<Header> headers = protocol.makeHeader( header, info );
            int nextLayer = 0;
            if (protocol.getNextProtocol() != null) {
               nextLayer = protocol.getNextProtocol().getLayer().getIndex();
            }
            
            for (Header h : headers) {
                expiredTime += send( nextLayer, h );
            }
        } else {
            expiredTime = send0( header );
            source.getEventScheduler().schedule( header );
        }
        
        return expiredTime;
    }
    
    /**
     * The "real" send operation.
     * 
     * @param header    the message to send.
    */
    private long send0( Header header )
    {
        System.out.println( "SENDING MESSAGE: " + header.getSizeInBits() );
        NetworkLink link = info.getLink();
        
        // TODO header.setLink( link );
        
        // TODO Utilizzare CSMA_CD per trasmettere il frame!!
        // TODO Il protocollo CSMA/CD viene utilizzato soltanto in caso di trasmissioni half-duplex,
        // TODO cioe' in link che accettano una sola direzione di trasmissione alla volta
        // TODO (ad esempio un hub e' un dispositivo half-duplex).
        
        // TODO Una connessione simplex significa invece che le parti sono sempre le stesse, MAI alternate.
        
        Time time = source.getTime();
        Time startTime = source.getTime();
        long expiredTime = link.getTtrasm( header.getSizeInBits() );
        Time endTime = startTime.clone().addTime( expiredTime, TimeUnit.MICROSECONDS );
        link.setUtilization( time, endTime.clone() );
        System.out.println( "T_TRASM: " + expiredTime );
        source.setTime( endTime );
        
        header.setTime( endTime );
        header.setSource( source );
        header.setDestination( dest );
        
        // TODO per essere completo bisognerebbe fargli stampare il contenuto del messaggio spedito
        // TODO cosi' da poterlo utilizzare nella simulazione grafica
        net.trackEvent( source.getId() + " " + startTime + " " + link.getDestId() + " " + time );
        
        return expiredTime;
    }
    
    @Override
    public void close()
    {
        protocols.clear();
    }
}
