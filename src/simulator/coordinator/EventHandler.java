
package simulator.coordinator;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import simulator.core.Event;
import simulator.core.Time;
import simulator.network.NetworkTopology;

public class EventHandler
{
    private NetworkTopology _network;
    
    private PriorityQueue<Event> events;
    /** Simulator time in microseconds. */
    private Time _time = new Time( 0, TimeUnit.MICROSECONDS );
    
    
    public EventHandler( final NetworkTopology network )
    {
        _network = network;
        events = new PriorityQueue<Event>();
    }
    
    /**
     * Get simulation time in microseconds.
     * @return
     */
    public Time now() {
        return _time;
    }
    
    public void doAllEvents()
    {
        Event e;
        while((e = events.poll()) != null) {
            if(_time.compareTo(e.getTime()) <= 0) {
                _time.setTime( e.getTime() );
            } else {
                //TODO throw new TimeException( "You can't go back in time!" );
            }
            e.execute( this );
        }
    }
    
    public void schedule( final Event e ) {
        events.add( e );
    }
    
    public void remove( final Event e ) {
        events.remove( e );
        //throw new UnsupportedOperationException();
    }
    
    public boolean hasNextEvents() {
        return !events.isEmpty();
    }
    
    public boolean isDone() {
        return false;
    }
}