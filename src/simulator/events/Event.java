/**
 * @author Stefano Ceccotti
*/

package simulator.events;

import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.events.EventHandler.EventType;
import simulator.events.impl.AgentEvent;
import simulator.events.impl.RequestEvent;
import simulator.events.impl.ResponseEvent;
import simulator.topology.NetworkLink;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public abstract class Event implements Comparable<Event>
{
    /** Event time in microseconds. */
    private Time _time = new Time( 0, TimeUnit.MICROSECONDS );
    private Time _arrivalTime = Time.ZERO;
    
    private Agent _source;
    private Agent _dest;
    
    private long _currentNodeId = 0;
    
    protected boolean _processedByAgent = true;
    
    // Message payload.
    protected Packet _packet;
    
    private long eventID     = -1;
    
    
    
    public Event( Time time )
    {
        _time.setTime( time );
        setId();
    }
    
    public Event( Time time, Agent source )
    {
        _time.setTime( time );
        _source = source;
        _currentNodeId = source.getId();
        setId();
    }
    
    public Event( Time time, Agent from, Agent to, Packet packet )
    {
        _time.setTime( time );
        _source = from;
        _currentNodeId = from.getId();
        _dest = to;
        
        _packet = packet;
        
        setId();
    }
    
    private void setId() {
        eventID = EventScheduler.nextEventId();
    }
    
    public Long getId() {
        return eventID;
    }
    
    public void setTime( Time time ) {
        _time.setTime( time );
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public void setArrivalTime( Time time ) {
        _arrivalTime = time;
    }
    
    public Time getArrivalTime() {
        return _arrivalTime;
    }
    
    public void setSource( Agent node ) {
        _source = node;
    }
    
    public Agent getSource() {
        return _source;
    }
    
    public Agent getDestination() {
        return _dest;
    }
    
    public void setDestination( Agent node ) {
        _dest = node;
    }
    
    public long getCurrentNodeId() {
        return _currentNodeId;
    }
    
    public void setPacket( Packet packet ) {
        _packet = packet;
    }
    
    public Packet getPacket() {
        return _packet;
    }
    
    /**
     * Execute this event.
     * 
     * @param evtScheduler    the event scheduler
     * @param net             the associate network
     * 
     * @return {@code true} if the event has been processed,
     *         {@code false} otherwise.
    */
    public void execute( EventScheduler evtScheduler, NetworkTopology net )
    {
        long nodeId = _currentNodeId;
        NetworkNode node = net.getNode( nodeId );
        if (!node.isActive()) {
            return;
        }
        
        if (!_processedByAgent) {
            evtScheduler.schedule( _source.fireEvent(/* null, null */) );
            // Analyze the attached packet.
            evtScheduler.schedule( node.analyzePacket( _packet ) );
            return;
        }
        
        //System.out.println( "EVENT: " + this + ", CURRENT: " + _currentNodeId );
        if (nodeId == _dest.getId()) {
            executeDestination( evtScheduler, net, node );
        } else {
            executeIntermediate( evtScheduler, net, node );
        }
    }
    
    private void executeDestination( EventScheduler evtScheduler, NetworkTopology net, NetworkNode node )
    {
        //System.out.println( "[" + _time + "] Reached destination node: " + node );
        setArrivalTime( _time.clone() );
        
        if (!checkEventExecution( _dest, evtScheduler )) {
            return;
        }
        
        // Compute the delay given by the time to process the incoming message.
        long delay = (_source.getId() == _dest.getId()) ?
                     0 : getTcalc( node, node.getAgent() ).getTimeMicros();
        _dest.setTime( _arrivalTime.clone().addTime( delay, TimeUnit.MICROSECONDS ) );
        
        if (this instanceof AgentEvent) {
            _dest.notifyEvent( this );
        } else {
            _dest.receivedMessage( this );
            _time.addTime( getTcalc( node, _dest ) );
        }
        
        evtScheduler.schedule( _dest.fireEvent() );
    }
    
    private void executeIntermediate( EventScheduler evtScheduler, NetworkTopology net, NetworkNode node )
    {
        long nodeId = node.getId();
        Time time = _time.clone();
        long nextNode = net.nextNode( nodeId, _dest.getId() ).getId();
        NetworkLink link = net.getLink( nodeId, nextNode );
        if (link != null && link.isActive()) {
            //if (nodeId == _source.getId()) System.out.println( "[" + _time + "] Starting from node: " + node );
            
            if (link.checkErrorLink()) {
                //System.out.println( "[" + _time + "] Packet lost due to an error in the link." );
                // TODO Use network protocols to manage the error (i.e. ICMPv6).
            } else {
                _currentNodeId = link.getDestId();
                long delay = 0;
                Agent agent = node.getAgent();
                if (nodeId != _source.getId()) {
                    //System.out.println( "[" + _time + "] Reached intermediate node: " + node );
                    if (!checkEventExecution( agent, evtScheduler )) {
                        return;
                    }
                    
                    delay = getTcalc( node, node.getAgent() ).getTimeMicros();
                    // Add event on queue only for nodes different from source.
                    agent.receivedMessage( this );
                }
                
                // Starting time is the current time + Tcalc of the node.
                Time startTime = _time.clone().addTime( delay, TimeUnit.MICROSECONDS );
                
                // If the transmission is not in parallel add the corresponding delay.
                if (!agent.isParallelTransmission()) {
                    long Ttrasm = link.getTtrasm( _packet.getSizeInBits() );
                    time.addTime( Ttrasm, TimeUnit.MICROSECONDS );
                    delay += Ttrasm;
                }
                
                // Here delay is the sum of Tcal (only for intermediate nodes) and Ttrasm.
                _time.addTime( delay, TimeUnit.MICROSECONDS );
                
                agent.setTime( agent.getTime().addTime( delay, TimeUnit.MICROSECONDS ) );
                if (agent.getEventHandler() != null) {
                    agent.getEventHandler().handle( this, EventType.SENT );
                }
                
                // The link propagation time is added here.
                _time.addTime( link.getTprop(), TimeUnit.MICROSECONDS );
                
                // Track the current event.
                net.trackEvent( nodeId + " " + startTime + " " + nextNode + " " + _time + " " + (this instanceof RequestEvent ? 1 : -1) );
                
                // Push-back the modified event into the queue.
                evtScheduler.schedule( this );
            }
            
            _source.setTime( time );
        }
        
        evtScheduler.schedule( _source.fireEvent() );
    }
    
    private Time getTcalc( NetworkNode node, Agent agent )
    {
        long delay;
        if (agent.getEventHandler() != null) {
            Time t = agent.getEventHandler().handle( this, EventType.RECEIVED );
            delay = t.getTimeMicros();
        } else {
            delay = node.getTcalc().getTimeMicros();
        }
        
        return new Time( delay, TimeUnit.MICROSECONDS );
    }
    
    /**
     * Checks whether the event can be executed.
    */
    private boolean checkEventExecution( Agent node, EventScheduler scheduler )
    {
        if (!node.addEvent( this )) {
            // No available space: the event will be discarded.
            // TODO notificarlo nel track event??
            return false;
        } else {
            if (!node.canExecute( _time )) {
                // Agent cannot handle the message in this moment.
                scheduler.schedule( this );
                return false;
            }
            node.removeEvent( this );
        }
        
        return true;
    }
    
    @Override
    public int compareTo( Event o )
    {
        int compare = _time.compareTo( o.getTime() );
        if (compare != 0) {
            return compare;
        } else {
            // If they have the same time compare their arrival time.
            compare = _arrivalTime.compareTo( o.getArrivalTime() );
            if (compare == 0) {
                // If they are of the same type compare their ID.
                if (this.getClass().equals( o.getClass() ))
                    return getId().compareTo( o.getId() );
                // Give priority to the incoming events.
                if (this instanceof ResponseEvent) return 1;
                else return -1;
            }
            return compare;
        }
    }

    @Override
    public String toString() {
        return "ID: " + eventID +
               ", From: {" + _source + "}, To: {" + _dest + "}" +
               ", Time: " + _time + "ns, arrival time: " + _arrivalTime + "ns";
    }
}
