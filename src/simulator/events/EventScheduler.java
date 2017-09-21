/**
 * @author Stefano Ceccotti
*/

package simulator.events;

import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import simulator.exception.TimeException;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public class EventScheduler
{
    private NetworkTopology _network;
    
    private AbstractQueue<Event> _events;
    /** Simulator time in microseconds. */
    private Time _time = new Time( 0, TimeUnit.MICROSECONDS );
    private Time _duration;
    
    private static long eventID = 0;
    
    
    public EventScheduler( final NetworkTopology network )
    {
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
                System.out.println( "TIME: " + _time + ", EVENT: " + e );
                throw new TimeException( "You can't go back in time!" );
            }
            
            e.execute( this, _network );
        }
    }
    
    public void schedule( final Event event )
    {
        if (event != null) {
            _events.add( event );
        }
    }
    
    public void schedule( final List<Event> events )
    {
        if (events != null) {
            _events.addAll( events );
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