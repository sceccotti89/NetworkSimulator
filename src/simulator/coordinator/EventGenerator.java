/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.coordinator.Event.RequestEvent;
import simulator.core.Time;

public abstract class EventGenerator
{
    protected Time _time;
    
    protected Time _duration;
    protected Time _departureTime;
    protected Time _serviceTime;
    
    protected Agent _from;
    protected Agent _to;
    
    protected boolean _waitResponse = true;
    
    
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final Time serviceTime )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration = duration;
        _departureTime = departureTime;
        _serviceTime = serviceTime;
    }
    
    public EventGenerator connect( final Agent from, final Agent to )
    {
        _from = from;
        _to = to;
        return this;
    }
    
    public EventGenerator setWaitReponse( final boolean flag ) {
        _waitResponse = flag;
        return this;
    }
    
    public boolean waitForResponse() {
        return _waitResponse;
    }
    
    //public abstract void update();

    //public abstract void generate( final EventHandler evHandler, final NetworkNode destNode );
    
    public Event generate()
    {
        //System.out.println( "TIME: " + _time + ", DURATION: " + _duration );
        //Time time = evHandler.getTime();
        _time.addTime( _departureTime );
        //System.out.println( "TIME: " + _time.getTimeMicroseconds() / 1000000 + ", DURATION: " + _duration.getTimeMicroseconds() / 1000000 );
        if(_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        //long nextTime = _time.getTimeMicroseconds() + _departureTime.getTimeMicroseconds();
        Event next = new RequestEvent( _time.clone(), _from, _to );
        return next;
    }
    
    
    
    
    public static abstract class ConstantEventGenerator extends EventGenerator
    {

        public ConstantEventGenerator( final Time duration,
                                       final Time departureTime,
                                       final Time serviceTime )
        {
            super( duration, departureTime, serviceTime );
            setWaitReponse( false );
        }
    }
    
    // TODO inserire quello richiesta-risposta
    
}