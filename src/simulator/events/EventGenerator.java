/**
 * @author Stefano Ceccotti
*/

package simulator.events;

import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.events.impl.AgentEvent;
import simulator.utils.Time;

public class EventGenerator
{
    protected Time _time;
    
    private Time   _duration;
    private Agent  _agent;
    private Time   _departureTime;
    
    private long   _id;
    
    private int    _departureAssignment;
    
    /** Decides whether the departure has to be added before the generation of the event. */
    public static final int BEFORE_CREATION = 0;
    /** Decides whether the departure has to be added after the generation of the event. */
    public static final int AFTER_CREATION = 1;
    
    
    /**
     * Creates a new event generator, generating events at time 0.
     * 
     * @param duration         lifetime of the generator.
     * @param departureTime    time to wait before sending a packet.
     * @param departureType    decides whether the departure time has to be added after
     *                         or before the generation of the event, using either
     *                         {@linkplain EventGenerator#BEFORE_CREATION} or
     *                         {@linkplain EventGenerator#AFTER_CREATION}.
     *                         In the latter case the events are generated starting from
     *                         the specified starting time
     *                         (see {@linkplain EventGenerator#startAt(Time)}).
    */
    public EventGenerator( Time duration, Time departureTime, int departureType )
    {
        _duration = duration;
        startAt( new Time( 0, TimeUnit.MICROSECONDS ) );
        setDepartureTime( departureTime, departureType );
    }
    
    public long getId() {
        return _id;
    }
    
    public void setAgent( Agent agent ) {
        _agent = agent;
    }
    
    public Agent getAgent() {
        return _agent;
    }
    
    /**
     * Sets the time when the generator starts at.
     * 
     * @param time    starting time.
    */
    public void startAt( Time time ) {
        _time.setTime( time );
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public void setDepartureTime( Time time, int departureType )
    {
        _departureAssignment = departureType;
        _departureTime = time;
    }

    /**
     * Returns the departure time of the next event from this node.</br>
     * Returning {@code null} makes the generator to produce no event.
     * 
     * @param e    the input event
     * 
     * @return the departure time.</br>
     * NOTE: returned time can be {@code null}.
    */
    protected Time computeDepartureTime( Event e ) {
        return _departureTime;
    }
    
    /**
     * Generates the next event.
    */
    protected Event createEvent() {
        return new AgentEvent( _time, _agent );
    }
    
    public final Event generate()
    {
        if (_time.compareTo( _duration ) > 0) {
            return null; // No more events from this generator.
        }
        
        Event event = null;
        
        if (_departureAssignment == AFTER_CREATION) {
            event = createEvent();
        }
        
        Time departureTime = computeDepartureTime( null );
        if (departureTime == null) {
            return null; // No events from this generator.
        }
        _time.addTime( departureTime );
        
        if (_departureAssignment == BEFORE_CREATION) {
            if (_time.compareTo( _duration ) > 0) {
                return null; // No more events from this generator.
            }
            event = createEvent();
        }
        
        return event;
    }
}
