/**
 * @author Stefano Ceccotti
*/

package simulator.events;

import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.events.EventHandler.EventType;
import simulator.events.impl.ResponseEvent;
import simulator.test.energy.Global;
import simulator.topology.NetworkLink;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public abstract class Event implements Comparable<Event>
{
    /** Event time in microseconds. */
    protected Time _time;
    protected Time _startTime;
    protected Time _arrivalTime = Time.ZERO;
    
    protected Agent _source;
    protected Agent _dest;
    
    protected long _prevNodeId = 0;
    protected long _currentNodeId = 0;
    
    protected boolean _processedByAgent = true;
    
    // Message payload.
    protected Packet _packet;
    
    private long eventID = -1;
    
    
    
    public Event( final Time time ) {
        _time = time.clone();
        setId();
    }
    
    public Event( final Time time, final Agent source ) {
        _time = time.clone();
        _source = source;
        _currentNodeId = source.getId();
        _prevNodeId = source.getId();
        setId();
    }
    
    public Event( final Time time, final Agent from, final Agent to, final Packet packet )
    {
        _time = time.clone();
        _source = from;
        _dest = to;
        
        _packet = packet;
        
        _currentNodeId = from.getId();
        _prevNodeId = from.getId();
        setId();
    }
    
    private void setId() {
        eventID = EventScheduler.nextEventId();
    }
    
    public Long getId() {
        return eventID;
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
        return _source;
    }
    
    public Agent getDestination() {
        return _dest;
    }
    
    public long getCurrentNodeId() {
        return _currentNodeId;
    }
    
    public Packet getPacket() {
        return _packet.clone();
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
    public void execute( final EventScheduler evtScheduler, final NetworkTopology net )
    {
        long nodeId = _currentNodeId;
        NetworkNode node = net.getNode( nodeId );
        if (!node.isActive()) {
            return;
        }
        
        if (!_processedByAgent) {
            evtScheduler.schedule( _source.fireEvent( null, null ) );
            // Analyze the attached packet.
            evtScheduler.schedule( node.analyzePacket( _packet ) );
            return;
        }
        
        if (nodeId != _source.getId()) {
            Global.eventWriter.println( _prevNodeId + " " + _startTime + " " + nodeId + " " + _time );
        }
        
        if (nodeId == _dest.getId()) {
            //Utils.LOGGER.debug( "[" + _time + "] Reached destination node: " + node );
            setArrivalTime( _time.clone() );
            _dest.addEventOnQueue( this );
            
            _time.addTime( getTcalc( node, _dest ) );
            _dest.removeEventFromQueue( 0 );
            
            // Prepare and schedule the response event.
            _dest.setTime( _arrivalTime );
            evtScheduler.schedule( _dest.fireEvent( _time, this ) );
        } else {
            long delay = 0;
            if (nodeId != _source.getId()) {
                //Utils.LOGGER.debug( "[" + _time + "] Reached intermediate node: " + node );
                delay = getTcalc( node, node.getAgent() ).getTimeMicroseconds();
            }
            
            Time time = _time.clone();
            NetworkLink link = net.getLink( nodeId, net.nextNode( nodeId, _dest.getId() ).getId() );
            if (link != null && link.isActive()) {
                /*if (nodeId == _source.getId()) {
                    Utils.LOGGER.debug( "[" + _time + "] Starting from node: " + node );
                }*/
                _startTime = _time.clone();
                
                if (link.checkErrorLink()) {
                    //System.out.println( "[" + _time + "] Packet lost due to an error in the link." );
                    // TODO utilizzare i vari protocolli per la gestione dell'errore (tipo ICMPv6).
                } else {
                    _prevNodeId = _currentNodeId;
                    _currentNodeId = link.getDestId();
                    long Ttrasm = link.getTtrasm( _packet.getSizeInBits() );
                    
                    Agent agent = node.getAgent();
                    if (nodeId != _source.getId()) {
                        // Add event on queue only for nodes different from source.
                        agent.addEventOnQueue( this );
                    }
                    // If the transmission is not in parallel add the corresponding delay.
                    if (!agent.parallelTransmission()) {
                        time.addTime( Ttrasm, TimeUnit.MICROSECONDS );
                        delay += Ttrasm;
                    }
                    
                    // Here delay is the sum of Tcal (only for intermediate nodes) and Ttrasm.
                    _time.addTime( delay, TimeUnit.MICROSECONDS );
                    
                    agent.setTime( agent.getTime().addTime( delay, TimeUnit.MICROSECONDS ) );
                    if (agent.getEventHandler() != null) {
                        agent.getEventHandler().handle( this, EventType.SENT );
                    }
                    if (nodeId != _source.getId()) {
                        agent.removeEventFromQueue( 0 );
                    }
                    
                    // The link propagation time is added here.
                    _time.addTime( link.getTprop(), TimeUnit.MICROSECONDS );
                    
                    // Push-back the modified event into the queue.
                    evtScheduler.schedule( this );
                }
            }
            
            _source.setTime( time );
            evtScheduler.schedule( _source.fireEvent( time, null ) );
        }
    }
    
    private Time getTcalc( final NetworkNode node, final Agent agent )
    {
        long delay;
        if (agent.getEventHandler() != null) {
            Time t = agent.getEventHandler().handle( this, EventType.RECEIVED );
            delay = t.getTimeMicroseconds();
        } else {
            delay = node.getTcalc().getTimeMicroseconds();
        }
        
        return new Time( delay, TimeUnit.MICROSECONDS );
    }
    
    @Override
    public int compareTo( final Event o )
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
                // Give priority to the outgoing events.
                if (this instanceof ResponseEvent) return 1;
                else return -1;
            }
            return compare;
        }
    }

    @Override
    public String toString() {
        return "ID: " + eventID + ", From: " + _source + ", To: " + _dest + ", Time: " +
               _time + "ns, arrival time: " + _arrivalTime + "ns";
    }
}