/**
 * @author Stefano Ceccotti
*/

package simulator.events;

import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import simulator.core.Simulator;
import simulator.exception.TimeException;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public class EventScheduler
{
    private final Simulator simulator;
    private NetworkTopology _network;
    
    private AbstractQueue<Event> _events;
    /** Simulator time in microseconds. */
    private Time _time = new Time( 0, TimeUnit.MICROSECONDS );
    private Time _duration;
    
    private static long eventID = 0;
    
    
    public EventScheduler( final Simulator simulator, final NetworkTopology network )
    {
        this.simulator = simulator;
        _network = network;
        _events = new PriorityBlockingQueue<Event>( 1 << 18 );
    }
    
    public void setNetwork( final NetworkTopology network ) {
        _network = network;
    }
    
    public NetworkTopology getNetwork() {
        return _network;
    }

    public void setDuration( final Time duration ) {
        _duration = duration;
    }
    
    /**
     * Gets simulation time in microseconds.
    */
    public Time getTime() {
        return _time;
    }
    
    public Time getTimeDuration() {
        return _duration.clone();
    }

    public void doAllEvents()
    {
        Event e;
        while ((e = _events.poll()) != null) {
            if (e.getTime().compareTo( _duration ) > 0) {
                break;
            }
            
            if (_time.compareTo( e.getTime() ) <= 0) {
                _time.setTime( e.getTime() );
            } else {
                // FIXME in caso di eventi da parte di altre reti c'e' il rischio che alcuni eventi
                // FIXME arrivino in tempi diversi tra di loro.
                throw new TimeException( "You can't go back in time!" );
            }
            
            e.execute( this, _network );
        }
    }
    
    public void schedule( final Event event )
    {
        if (event != null) {
            long nodeId = -1;
            if (event.getDestination() != null) {
                nodeId = event.getDestination().getNode().getId();
            }
            
            if (nodeId < 0 || _network.containsNode( nodeId )) {
                _events.add( event );
            } else {
                changeScheduler( nodeId, event );
            }
        }
    }
    
    public void schedule( final List<Event> events )
    {
        if (events != null) {
            long nodeId = -1;
            if (events.get( 0 ).getDestination() != null) {
                nodeId = events.get( 0 ).getDestination().getNode().getId();
            }
            if (nodeId < 0 || _network.containsNode( nodeId )) {
                _events.addAll( events );
            } else {
                changeScheduler( nodeId, events );
            }
        }
    }
    
    private void scheduleFromOtherNetwork( final Event event ) {
        if (event != null) {
            // TODO dovrebbe risvegliare lo scheduler se e' in attesa di altri eventi
            _events.add( event );
        }
    }
    
    private void scheduleFromOtherNetwork( final List<Event> events ) {
        if (events != null) {
            // TODO dovrebbe risvegliare lo scheduler se e' in attesa di altri eventi
            _events.addAll( events );
        }
    }
    
    /**
     * Change the scheduler for the given event.
     * 
     * @param nodeId    node associated to the event.
     * @param event     the given event.
    */
    private void changeScheduler( final long nodeId, final Event event )
    {
        for (NetworkTopology net : simulator.getNetworks()) {
            if (net.containsNode( nodeId )) {
                net.getEventScheduler().scheduleFromOtherNetwork( event );
                break;
            }
        }
    }
    
    /**
     * Change the scheduler for the given events.
     * 
     * @param nodeId    node associated to the events.
     * @param events    the given events.
    */
    private void changeScheduler( final long nodeId, final List<Event> events )
    {
        for (NetworkTopology net : simulator.getNetworks()) {
            if (net.containsNode( nodeId )) {
                net.getEventScheduler().scheduleFromOtherNetwork( events );
                break;
            }
        }
    }

    /**
     * Removes the input event from the queue.
     * 
     * @param e    event to remove
     * 
     * @return {@code true} if the event has been successfully removed,
     *         {@code false} otherwise.
    */
    public boolean remove( final Event e ) {
        if (e != null) {
            return _events.remove( e );
        }
        return false;
    }

    public boolean hasNextEvents() {
        return !_events.isEmpty();
    }
    
    /**
     * Returns the next unique event identifier.
    */
    public static long nextEventId() {
        return ++eventID % Long.MAX_VALUE;
    }
    
    public void shutdown() {
        _events.clear();
    }
}