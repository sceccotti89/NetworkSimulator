/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.coordinator.Event.RequestEvent;
import simulator.core.Time;
import simulator.network.NetworkNode;

public abstract class EventGenerator
{
    protected Time _time;
    
    protected Time _duration;
    protected Time _departureTime;
    protected Time _serviceTime;
    
    protected Agent _from;
    protected Agent _to;
    
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final Time serviceTime )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration = duration;
        _departureTime = departureTime;
        _serviceTime = serviceTime;
    }
    
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final Time serviceTime,
                           final Agent from,
                           final Agent to )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration = duration;
        _departureTime = departureTime;
        _serviceTime = serviceTime;
        
        setLink( from, to );
    }
    
    public void setLink( final Agent from, final Agent to )
    {
        _from = from;
        _to = to;
    }
    
    public abstract void generate( final EventHandler evHandler, final NetworkNode destNode );
    
    public Event nextEvent()
    {
        //Time time = evHandler.getTime();
        if(_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        _time.addTime( _departureTime );
        //long nextTime = _time.getTimeMicroseconds() + _departureTime.getTimeMicroseconds();
        Event next = new RequestEvent( _time.clone(), _from, _to );
        return next;
    }
}