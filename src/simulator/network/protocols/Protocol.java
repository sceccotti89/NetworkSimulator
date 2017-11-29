/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.network.ConnectionInfo;
import simulator.network.NetworkLayer;
import simulator.network.protocols.Header.Range;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public abstract class Protocol
{
    protected final NetworkTopology net;
    protected Time time;
    protected NetworkLayer layer;
    protected Agent node;
    protected Protocol nextProtocol;
    
    
    public static final String SEPARATOR = "@";
    
    
    
    public Protocol( final NetworkLayer layer, final Protocol... baseProtocols ) {
        this( layer, null, baseProtocols );
    }
    
    public Protocol( final NetworkLayer layer, final NetworkTopology net, final Protocol... baseProtocols )
    {
        time = new Time( 0, TimeUnit.MICROSECONDS );
        this.layer = layer;
        this.net = net;
    }
    
    public NetworkLayer getLayer() {
        return layer;
    }
    
    public void setAgent( final Agent node ) {
        this.node = node;
    }
    
    public Agent getAgent() {
        return node;
    }
    
    public void setNextProtocol( final Protocol protocol ) {
        nextProtocol = protocol;
    }
    
    /**
     * Returns the inner protocol.
    */
    public Protocol getNextProtocol() {
        return nextProtocol;
    }
    
    /**
     * Returns the bits contained in the given range.
     * 
     * @param header    the specified header.
     * @param range     the given range.
    */
    protected String getBitField( final Header header, final Range range )
    {
        String value = "";
        for (int i = 0; i < range.length(); i++) {
            value += header.getField( range.from() + i, 1 );
        }
        return value;
    }
    
    /**
     * Returns the headers generated by this protocol,
     * based on the given informations.
     * 
     * @param upperHeader    the header of the upper layer.
     * @param info           contains infos about the current connection.
    */
    public abstract List<Header> makeHeader( final Header upperHeader, final ConnectionInfo info );
    
    /**
     * Process an incoming header.
     * 
     * @param header    the input header.
     * 
     * @return an object containing either the protocol used in the upper layer or the response message (if any) or both,
     *         {@code null} otherwise.
    */
    public abstract ProtocolReference processHeader( final Header header );
    
    /**
     * Prints out the contents of the given header.
     * 
     * @param header    the header.
    */
    public abstract void printHeader( final Header header );
    
    /**
     * Returns the name of the protocol.
     * 
     * @param extended    if {@code true} returns the extended version of the name,
     *                    {@code false} only the acronym.
    */
    public abstract String getName( final boolean extended );
    
    /**
     * Returns a map of fields containing their current values.</br>
     * The order in which the fields are added into the linked map is maintained
     * when it's iterated.
     * 
     * @param header    the header.
    */
    public abstract LinkedHashMap<String, String> getFields( final Header header );
}