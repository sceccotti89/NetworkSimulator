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
    public Time getTime() {
        return _time;
    }

    public void doAllEvents()
    {
        long index = 0;
        Event e;
        while ((e = events.poll()) != null) {
            //System.out.println( e );
            if(_time.compareTo(e.getTime()) <= 0) {
                _time.setTime( e.getTime() );
            } else {
                throw new TimeException( "You can't go back in time!" );
            }
            
            System.out.println( "EVENT No: " + (++index) );
            e.execute( e._currentNodeId, this, _network );
            try {
                Thread.sleep( 1000 );
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }
    
    public void schedule( final Event e )
    {
        if (e != null) {
            events.add( e );
        }
    }
    
    public void remove( final Event e )
    {
        if (e != null)
            events.remove( e );
    }
    
    public boolean hasNextEvents() {
        return !events.isEmpty();
    }
    
    public boolean isDone() {
        return false;
    }
}