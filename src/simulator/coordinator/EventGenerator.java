/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.Packet;
import simulator.coordinator.Event.RequestEvent;
import simulator.core.Time;

public abstract class EventGenerator
{
    protected Time _time;
    
    protected Time _duration;
    protected Time _departureTime;
    protected Packet _packet;
    
    protected Agent _from;
    protected Agent _to;
    
    protected boolean _waitResponse = true;
    
    
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final Packet packet )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration = duration;
        _departureTime = departureTime;
        _packet = packet;
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
    
    public Event generate( final Time t )
    {
        //System.out.println( "MY_TIME: " + _time + ", INPUT_TIME: " + t );
        if (t != null && waitForResponse())
            _time = t;
        
        //System.out.println( "TIME: " + _time + ", DURATION: " + _departureTime );
        //Time time = evHandler.getTime();
        _time.addTime( _departureTime );
        //System.out.println( "TIME: " + _time.getTimeMicroseconds() / 1000000 + ", DURATION: " + _duration.getTimeMicroseconds() / 1000000 );
        if(_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        //long nextTime = _time.getTimeMicroseconds() + _departureTime.getTimeMicroseconds();
        Event next = new RequestEvent( _time.clone(), _from, _to, _packet );
        return next;
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    




    public static abstract class ConstantEventGenerator extends EventGenerator
    {
        
        public ConstantEventGenerator( final Time duration,
                                       final Time departureTime,
                                       final Packet pktSize )
        {
            super( duration, departureTime, pktSize );
            setWaitReponse( false );
        }
    }
    
    // TODO Gestire in maniera efficace anche questo generatore
    // TODO prima di tutto devo inserire una variabile che conti il numero di pacchetti in volo
    // TODO se il numero e' minore di una certa soglia allora spedisco il prossimo messaggio,
    // TODO altrimenti aspetto una risposta per almeno uno di essi prima di andare avanti.
    public static abstract class ResponseEventGenerator extends EventGenerator
    {
        
        public ResponseEventGenerator( final Time duration,
                                       final Time departureTime,
                                       final Packet pktSize )
        {
            super( duration, departureTime, pktSize );
            setWaitReponse( true );
        }
    }
}