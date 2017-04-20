/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import simulator.core.Time;
import simulator.exception.TimeException;
import simulator.network.NetworkTopology;

public class EventScheduler
{
    private NetworkTopology _network;
    
    //private Queue<Event> _events;
    private NavigableSet<Event> _events;
    /** Simulator time in microseconds. */
    private Time _time = new Time( 0, TimeUnit.MICROSECONDS );
    private Time _duration;
    
    private static long eventID = 0;
    
    
    public EventScheduler( final NetworkTopology network )
    {
        _network = network;
        //_events = new PriorityQueue<Event>();
        _events = new TreeSet<Event>();
    }
    
    public void setNetworkTopology( final NetworkTopology network ) {
        _network = network;
    }
    
    public void setDuration( final Time duration ) {
        _duration = duration;
    }
    
    /**
     * Get simulation time in microseconds.
     * @return
    */
    public Time getTime() {
        return _time;
    }
    
    public void doAllEvents()
    {
        Event e;
        while ((e = _events.pollFirst()) != null) {
            if (e.getTime().compareTo( _duration ) > 0) {
                break;
            }
            
            if (_time.compareTo( e.getTime() ) <= 0) {
                _time.setTime( e.getTime() );
            } else {
                throw new TimeException( "You can't go back in time!" );
            }
            
            if (e.execute( this, _network )) {
                // TODO REMOVE AFTER TESTS
                try {
                    Thread.sleep( 000 );
                } catch ( InterruptedException e1 ) {
                    e1.printStackTrace();
                }
            }
        }
    }
    
    public void schedule( final Event event ) {
        //System.out.println( "GENERATED: " + event );
        if (event != null) {
            _events.add( event );
        }
        //System.out.println( "SCHEDULED: " + _events );
    }
    
    public void schedule( final List<Event> events ) {
        //System.out.println( "GENERATED: " + events );
        if (events != null) {
            _events.addAll( events );
        }
        //System.out.println( "SCHEDULED: " + _events );
    }
    
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
        return ++eventID;
    }
}