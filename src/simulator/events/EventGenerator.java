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
    
    
    /**
     * Creates a new event generator.
     * 
     * @param duration              lifetime of the generator.
     * @param departureTime         time to wait before sending a packet.
    */
    public EventGenerator( Time duration, Time departureTime )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration      = duration;
        _departureTime = departureTime;
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
     * Sets the time when the generator starts at.</br>
     * This method lets the generator to send events in any moment.
     * 
     * @param time    starting time.
    */
    public void startAt( Time time ) {
        _time.setTime( time );
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public void setDepartureTime( Time time ) {
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
            return null;
        }
        
        Event event = createEvent();
        
        // FIXME ora come ora i generatori attivi perdono il tempo di invio del
        // FIXME precedente evento, cioe' e' in ritardo di 1.
        // FIXME al fine del simulatore cambia poco ma dovrei trovare un modo per sistemarlo.
        
        Time departureTime = computeDepartureTime( event );
        if (departureTime == null) {
            return null; // No events from this generator.
        }
        
        _time.addTime( departureTime );
        
        return event;
    }
}
