/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import simulator.core.Time;
import simulator.exception.TimeException;
import simulator.network.NetworkTopology;

public class EventScheduler
{
    private NetworkTopology _network;
    
    private PriorityQueue<Event> _events;
    /** Simulator time in microseconds. */
    private Time _time = new Time( 0, TimeUnit.MICROSECONDS );
    
    
    public EventScheduler( final NetworkTopology network )
    {
        _network = network;
        _events = new PriorityQueue<Event>();
    }
    
    public void setNetworkTopology( final NetworkTopology network ) {
        _network = network;
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
        long index = 0;
        Event e;
        while ((e = _events.poll()) != null) {
            if (_time.compareTo( e.getTime() ) <= 0) {
                _time.setTime( e.getTime() );
            } else {
                throw new TimeException( "You can't go back in time!" );
            }
            
            System.out.println( "EVENT No: " + (++index) );
            e.execute( e._currentNodeId, this, _network );
            try {
                Thread.sleep( 000 );
            } catch ( InterruptedException e1 ) {
                // TODO Auto-generated catch block: REMOVE THIS AFTER TESTS
                e1.printStackTrace();
            }
        }
    }
    
    public void schedule( final List<Event> events )
    {
        if (events != null) {
            _events.addAll( events );
        }
    }
    
    public void remove( final Event e )
    {
        if (e != null)
            _events.remove( e );
    }
    
    public boolean hasNextEvents() {
        return !_events.isEmpty();
    }
    
    public boolean isDone() {
        return false;
    }
}