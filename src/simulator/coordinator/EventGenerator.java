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
    protected Time _duration;
    protected Time _departureTime;
    protected Time _serviceTime;
    
    protected Agent _from;
    protected Agent _to;
    
    public EventGenerator( final Time duration,
            final Time departureTime,
            final Time serviceTime )
    {
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
    
    public Event nextEvent( final EventHandler evHandler )
    {
        Time time = evHandler.getTime();
        if(time.compareTo( _duration ) > 0)
            return null; // Stop the generator.
        
        long nextTime = time.getTimeMicroseconds() + _departureTime.getTimeMicroseconds();
        Event next = new RequestEvent( new Time( nextTime, TimeUnit.MICROSECONDS ), _from, _to );
        return next;
    }
    
    // TODO Fornire di seguito alcuni generatori gia' pre-costruiti?? tipo il CBR, TCP, UDP, etc..
}