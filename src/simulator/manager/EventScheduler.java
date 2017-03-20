/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.util.ArrayList;
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
    
    // TODO Mettere un delay di qualche microsecondo: concordare con il proffe.
    // TODO anche se formalmente il delay andrebbe calcolato in base al tipo di calcolo da eseguire.
    private static final Time DELAY = new Time( 500, TimeUnit.MICROSECONDS );
    
    
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
        long index = 1;
        Event e;
        while ((e = _events.poll()) != null) {
            if (_time.compareTo( e.getTime() ) <= 0) {
                _time.setTime( e.getTime() );
            } else {
                throw new TimeException( "You can't go back in time!" );
            }
            
            e.setId( index++ );
            System.out.println( "EVENT No: " + index );
            e.execute( e.getCurrentNodeId(), this, _network );
        }
    }
    
    /**
     * Checks in the queue if the time of an event lies between the current time
     * and a delayed one, where delay is evaluated in some microseconds.
     * 
     * @param id      identifier of the calling node
     * @param time    time of the calling node
     * 
     * @return list of events associated to the input node.
    */
    public List<Event> checkForNearEvents( final long id, final Time time )
    {
        Time delayed = time.clone().addTime( DELAY );
        List<Event> nodeEvents = null;
        System.out.println( "TIME: " + time + ", DELAYED: " + delayed );
        for (Event e : _events) {
            if (e.getCurrentNodeId() == id && e.getDest().getId() == id && e.getTime().compareTo( delayed ) <= 0) {
                // Founded an event whose destination is the input one.
                System.out.println( "E_TIME: " + e.getTime() + ", CURRENT: " + e.getCurrentNodeId() + ", IN_ID: " + id );
                if (nodeEvents == null)
                    nodeEvents = new ArrayList<>();
                nodeEvents.add( e );
            }
        }
        return nodeEvents;
    }
    
    public void schedule( final Event event ) {
        if (event != null) {
            _events.add( event );
        }
    }
    
    public void schedule( final List<Event> events ) {
        if (events != null) {
            _events.addAll( events );
        }
    }
    
    public void remove( final Event e ) {
        if (e != null)
            _events.remove( e );
    }
    
    public boolean hasNextEvents() {
        return !_events.isEmpty();
    }
}