/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.Packet;
import simulator.core.Time;
import simulator.manager.EventHandler.EventType;
import simulator.network.NetworkLink;
import simulator.network.NetworkNode;
import simulator.network.NetworkTopology;

public abstract class Event implements Comparable<Event>
{
    /** Event time in microseconds. */
    protected Time _time;
    protected Time _arrivalTime;
    
    protected Agent _from;
    protected Agent _to;
    
    protected long _currentNodeId = 0;
    
    private boolean _firstArrive = true;
    
    // Message payload.
    protected Packet _packet;
    
    private long eventID = -1;
    
    
    public Event( final Time time, final Agent from, final Agent to, final Packet packet )
    {
        _time = time.clone();
        _from = from;
        _to = to;
        
        _packet = packet;
        
        _currentNodeId = from.getId();
        setId();
    }
    
    public void setId() {
        eventID = EventScheduler.nextEventId();
    }
    
    public long getId() {
        return eventID;
    }
    
    @Override
    public int compareTo( final Event o )
    {
        int compare = _time.compareTo( o.getTime() );
        if (compare != 0) {
            return compare;
        } else {
            // If they have the same time, compare they arrival time.
            if (_arrivalTime == null && o.getArrivalTime() == null) return 0;
            if (_arrivalTime != null && o.getArrivalTime() == null) return -1;
            if (_arrivalTime == null && o.getArrivalTime() != null) return 1;
            return _arrivalTime.compareTo( o.getArrivalTime() );
        }
    }
    
    public void setTime( final Time time ) {
        _time = time;
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public void setArrivalTime( final Time time ) {
        _arrivalTime = time;
    }
    
    public Time getArrivalTime() {
        return _arrivalTime;
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
    
    /**
     * Verify if the current event can be processed immediately,
     * or if it has to be reinserted into the queue.
     * 
     * @param ev_handler    the event handler
     * @param net           the associate network
     * 
     * @return {@code true} if the event has been processed,
     *         {@code false} otherwise.
    */
    final public boolean execute( final EventScheduler ev_handler, final NetworkTopology net )
    {
        long nodeId = _currentNodeId;
        NetworkNode node = net.getNode( nodeId );
        
        System.out.println( "\nNode: " + node.getName() + ", execute: " + this );
        
        if (nodeId == _to.getId()) {
            System.out.println( "EV_TIME: " + _time + ", DEST_TIME: " + _to.getTime() );
            if (_firstArrive) {
                setArrivalTime( _time.clone() );
                System.out.println( "[" + _arrivalTime + "] Reached destination node: " + node );
                _firstArrive = false;
                _to.addEventOnQueue( this );
            }
            
            if (!_to.canExecute( _time )) {
                // Reinsert the query to the scheduler.
                System.out.println( _time + " = REINSERT: " + this );
                ev_handler.schedule( this );
                return false;
            }
            
            _time.addTime( getTcalc( node, _to ) );
            _to.removeEventFromQueue( 0 );
            
            // Prepare and schedule the response event.
            _to.setTime( _arrivalTime );
            ev_handler.schedule( _to.fireEvent( _time, this ) );
        } else {
            long delay = 0;
            if (nodeId == _from.getId()) {
                System.out.println( "[" + _time + "] Starting from node: " + node );
            } else {
                System.out.println( "[" + _time + "] Reached intermediate node: " + node );
                delay = getTcalc( node, node.getAgent() ).getTimeMicroseconds();
            }
            
            NetworkLink link = net.getLink( nodeId, net.nextNode( nodeId, _to.getId() ).getId() );
            if (link != null) {
                // Assign the current node id.
                _currentNodeId = link.getDestId();
                long Ttrasm = link.getTtrasm( _packet.getSizeInBits() );
                
                Agent agent = node.getAgent();
                
                Time time = _time.clone();
                if (agent != null) {
                    if (nodeId != _from.getId()) {
                        // Add event on queue only for intermediate nodes.
                        agent.addEventOnQueue( this );
                    }
                    // If the transmission is not in parallel add the corresponding delay.
                    if (!agent.parallelTransmission()) {
                        time.addTime( Ttrasm, TimeUnit.MICROSECONDS );
                        delay += Ttrasm;
                    }
                }
                
                _from.setTime( time );
                ev_handler.schedule( _from.fireEvent( time, null ) );
                
                // Here delay is the sum of Tcal (only for intermediate nodes) and Ttrasm.
                _time.addTime( delay, TimeUnit.MICROSECONDS );
                
                if (agent != null) {
                    agent.setTime( agent.getTime().addTime( delay, TimeUnit.MICROSECONDS ) );
                    if (agent.getEventHandler() != null) {
                        agent.getEventHandler().handle( this, EventType.RECEIVED );
                    }
                    if (nodeId != _from.getId()) {
                        agent.removeEventFromQueue( 0 );
                    }
                }
                
                // The link propagation time is added here.
                _time.addTime( link.getTprop(), TimeUnit.MICROSECONDS );
                
                //System.out.println( "Ttrasm: " + ((double) Ttrasm)/((double) Utils.MILLION) + "s" );
                //System.out.println( "Tprop:  " + ((double) link.getTprop())/((double) Utils.MILLION) + "s" );
                
                // Push-back the modified event into the queue.
                ev_handler.schedule( this );
            }
        }
        
        return true;
    }
    
    private Time getTcalc( final NetworkNode node, final Agent agent )
    {
        long delay;
        if (agent.getEventHandler() != null) {
            delay = agent.getEventHandler().handle( this, EventType.SENT ).getTimeMicroseconds();
        } else {
            delay = node.getTcalc().getTimeMicroseconds();
        }
        
        return new Time( delay, TimeUnit.MICROSECONDS );
    }
    
    @Override
    public String toString() {
        return eventID + ", From: " + _from + ", To: " + _to + ", Time: " +
               _time + "ns, arrival time: " + ((_arrivalTime == null) ? 0 : _arrivalTime) + "ns";
    }
    
    
    /** ======= SPECIALIZED IMPLEMENTATIONS OF EVENT ======= **/
    
    public static class RequestEvent extends Event
    {
        public RequestEvent( final Time time, final Agent from, final Agent to, final Packet pkt )
        {
            super( time, from, to, pkt );
        }
        
        @Override
        public String toString() {
            return "Request Event: " + super.toString();
        }
    }
    
    public static class ResponseEvent extends Event
    {
        public ResponseEvent( final Time time, final Agent from, final Agent to, final Packet packet )
        {
            super( time, from, to, packet );
        }
        
        @Override
        public String toString() {
            return "Response Event: " + super.toString();
        }
    }
}