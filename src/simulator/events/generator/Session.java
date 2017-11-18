/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.core.Agent;
import simulator.events.Packet;

public class Session
{
    private final long _id;
    
    private long _sentPackets     = 0;
    private long _receivedPackets = 0;
    private long _maxPacketsInFlight = 0;
    
    private Agent _source;
    
    private Packet _reqPacket = null;
    
    private int     _nextDestIndex = -1;
    
    /* Flow identifier. */
    private static int flowID = -1;
    
    
    
    public Session() {
        this( nextFlowID() );
    }
    
    public Session( long id ) {
        _id = id;
    }
    
    /**
     * Returns the flow identifier.
    */
    public long getId() {
        return _id;
    }
    
    public void setMaximumFlyingPackets( long max ) {
        _maxPacketsInFlight = max;
    }
    
    /**
     * Sets the request packet associated with this session.
    */
    public void setPacket( Packet packet ) {
        _reqPacket = packet;
    }
    
    /**
     * Returns the request packet associated with this session.
    */
    public Packet getPacket() {
        return _reqPacket;
    }
    
    public void setSource( Agent agent ) {
        _source = agent;
    }
    
    public Agent getSource() {
         return _source;
    }
    
    public void increaseSentPackets() {
        _sentPackets++;
    }
    
    public void increaseReceivedPackets() {
        _receivedPackets++;
    }
    
    /**
     * Returns the next destination index.
    */
    public int getNextDestination( int destinations ) {
        return _nextDestIndex = (_nextDestIndex + 1) % destinations;
    }
    
    /**
     * Checks whether the generator can send other packets.
    */
    public boolean canSend() {
        return _sentPackets < _maxPacketsInFlight;
    }
    
    /**
     * Checks whether the session has been completed.
    */
    public boolean completed() {
        return _receivedPackets == _maxPacketsInFlight;
    }

    /**
     * Returns the next unique flow identifier.
    */
    private static synchronized long nextFlowID() {
        return ++flowID  % Long.MAX_VALUE;
    }
}
