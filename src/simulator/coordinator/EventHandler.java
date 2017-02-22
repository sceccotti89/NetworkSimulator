/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import simulator.core.Time;
import simulator.exception.TimeException;
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
    
    public void setNetworkTopology( final NetworkTopology network ) {
        _network = network;
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
        int index = 0;
        Event e;
        while((e = events.poll()) != null) {
            System.out.println( e );
            if(_time.compareTo(e.getTime()) <= 0) {
                _time.setTime( e.getTime() );
            } else {
                throw new TimeException( "You can't go back in time!" );
            }
            
            System.out.println( "EVENT N°: " + (++index) );
            long _id = _network.nextNode( e._currentNodeId );
            e.execute( _id, this );
        }
    }
    
    public void schedule( final Event e )
    {
        if(e != null)
            events.add( e );
    }
    
    public void remove( final Event e ) {
        events.remove( e );
    }
    
    public boolean hasNextEvents() {
        return !events.isEmpty();
    }
    
    public boolean isDone() {
        return false;
    }

    public Time getTime() {
        return _time;
    }
}