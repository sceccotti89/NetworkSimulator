/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.Packet;
import simulator.core.Time;
import simulator.network.NetworkLink;
import simulator.network.NetworkNode;
import simulator.network.NetworkTopology;
import simulator.utils.Utils;

public abstract class Event implements Comparable<Event>
{
    /** Event time in microseconds. */
    protected Time _time;
    
    protected Agent _from;
    protected Agent _to;
    
    protected long _currentNodeId = 0;
    
    // Message payload.
    protected Packet _packet;
    
    private long eventID = -1;
    
    
    public Event( final Time time, final Agent from, final Agent to, final Packet packet )
    {
        _time = time;
        _from = from;
        _to = to;
        
        _packet = packet;
        
        _currentNodeId = from.getId();
    }
    
    public void setId( final long id ) {
        if (eventID == -1) {
            eventID = id;
        }
    }
    
    public long getId() {
        return eventID;
    }
    
    @Override
    public int compareTo( final Event o ) {
        return _time.compareTo( o.getTime() );
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public Agent getSource() {
        return _from;
    }
    
    public Agent getDest() {
        return _to;
    }
    
    public long getCurrentNodeId() {
        return _currentNodeId;
    }
    
    public Packet getPacket() {
        return _packet.clone();
    }
    
    final public void execute( final long nodeId, final EventScheduler ev_handler, final NetworkTopology net )
    {
        NetworkNode node = net.getNode( nodeId );
        
        if (nodeId == _to.getId()) {
            System.out.println( "[" + _time + "] Reached destination node: " + node );
            
            _to.setElapsedTime( _time.getTimeMicroseconds() );
            _to.addEventOnQueue( this );
            
            System.out.println( ev_handler.checkForNearEvents( _to.getId(), _to.getTime() ) );
            List<Event> nodeEvents = ev_handler.checkForNearEvents( _to.getId(), _to.getTime() );
            if (nodeEvents != null) {
                // Put the retrieved events into the destination queue.
                for (Event e: nodeEvents) {
                    _to.addEventOnQueue( e );
                }
            }
            
            if (node.getTcalc().isDynamic() && _to != null) {
                _time.addTime( _to.analyzeEvent( _time, this ) );
            } else {
                _time.addTime( node.getTcalc() );
            }
            // Prepare and schedule the response event.
            ev_handler.schedule( _to.fireEvent( _time, this ) );
        } else {
            long delay = 0;
            if (nodeId == _from.getId()) {
                System.out.println( "[" + _time + "] Starting from node: " + node );
            } else {
                System.out.println( "[" + _time + "] Reached intermediate node: " + node );
                if (node.getTcalc().isDynamic() && node.getAgent() != null) {
                    delay = node.getAgent().analyzeEvent( _time, this ).getTimeMicroseconds();
                } else {
                    delay = node.getTcalc().getTimeMicroseconds();
                }
            }
            
            NetworkLink link = net.getLink( nodeId, net.nextNode( nodeId, _to.getId() ).getId() );
            if (link != null) {
                // Assign the current node id.
                _currentNodeId = link.getDestId();
                long Ttrasm = link.getTtrasm( _packet.getSizeInBits() );
                
                Time time = _time.clone().addTime( Ttrasm, TimeUnit.MICROSECONDS );
                ev_handler.schedule( _from.fireEvent( time, null ) );
                
                delay += Ttrasm + link.getTprop();
                
                System.out.println( "Ttrasm: " + ((double) Ttrasm)/((double) Utils.MILLION) + "s" );
                System.out.println( "Tprop:  " + ((double) link.getTprop())/((double) Utils.MILLION) + "s" );
                _time.addTime( delay, TimeUnit.MICROSECONDS );
                
                // Push-back the modified event into the queue.
                ev_handler.schedule( this );
            }
        }
    }
    
    
    
    
    /** ======= SPECIALIZED IMPLEMENTATIONS OF EVENT ======= **/
    
    public static class RequestEvent extends Event
    {
        public RequestEvent( final Time time, final Agent from, final Agent to, final Packet pkt )
        {
            super( time, from, to, pkt );
        }
        
        @Override
        public String toString()
        { return "Request Event: " + _time + "ns"; }
    }
    
    public static class ResponseEvent extends Event
    {
        public ResponseEvent( final Time time, final Agent from, final Agent to, final Packet packet )
        {
            super( time, from, to, packet );
        }
        
        @Override
        public String toString()
        { return "Response Event: " + _time + "ns"; }
    }
}