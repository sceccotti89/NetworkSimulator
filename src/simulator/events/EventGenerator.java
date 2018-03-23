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
    
    private int    _departureAssignment;
    
    /** Adds the departure time to the generator before the generation of the event. */
    public static final int BEFORE_CREATION = 0;
    /** Adds the departure time to the generator after the generation of the event. */
    public static final int AFTER_CREATION = 1;
    
    
    /**
     * Creates a new event generator, generating events at time 0.
     * 
     * @param duration         lifetime of the generator.
     * @param departureTime    time to wait before generating a new event.
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
    
    public void setAgent( Agent agent ) {
        _agent = agent;
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
     * @param e    the last event generated by this generator.</br>
     *             In case of {@link EventGenerator#BEFORE_CREATION} the event in {@code null}.
     * 
     * @return the departure time.
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
        
        Time departureTime = computeDepartureTime( event );
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
