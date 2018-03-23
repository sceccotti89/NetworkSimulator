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
    
    private Time _duration;
    private Agent _agent;
    private Time    _departureTime;
    
    private long _id;
    
    private int departureAssignment;
    
    /** Decides whether the departure has to be added to the generated event. */
    public static final int BEFORE_CREATION = 0, AFTER_CREATION = 1;
    
    
    /**
     * Creates a new event generator.
     * 
     * @param duration         lifetime of the generator.
     * @param departureTime    time to wait before sending a packet.
     * @param departureType    
    */
    public EventGenerator( Time duration, Time departureTime, int departureType )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration = duration;
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
        departureAssignment = departureType;
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
        
        if (departureAssignment == AFTER_CREATION) {
            event = createEvent();
        }
        
        Time departureTime = computeDepartureTime( null );
        if (departureTime == null) {
            return null; // No events from this generator.
        }
        _time.addTime( departureTime );
        
        if (departureAssignment == BEFORE_CREATION) {
            if (_time.compareTo( _duration ) > 0) {
                return null; // No more events from this generator.
            }
            event = createEvent();
        }
        
        return event;
    }
}
