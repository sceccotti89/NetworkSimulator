/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.Packet;
import simulator.coordinator.Event.RequestEvent;
import simulator.coordinator.Event.ResponseEvent;
import simulator.core.Time;
import simulator.utils.SimulatorUtils;

public abstract class EventGenerator
{
    protected Time _time;
    
    protected Time _duration;
    protected Time _departureTime;
    protected Packet _reqPacket;
    protected Packet _resPacket;
    
    protected Agent _from;
    protected Agent _to;
    
    protected boolean _activeGenerator = false;
    protected boolean _waitResponse = true;
    protected long _packetsInFly = 0;
    protected long _maxPacketsInFly = 0;
    
    
    
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final long maxPacketsInFly,
                           final Packet reqPacket,
                           final Packet resPacket,
                           final boolean isActive,
                           final boolean waitResponse )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration        = duration;
        _departureTime   = departureTime;
        _maxPacketsInFly = maxPacketsInFly;
        _reqPacket       = reqPacket;
        _resPacket       = resPacket;
        _activeGenerator = isActive;
        _waitResponse    = waitResponse;
    }
    
    public EventGenerator connect( final Agent from, final Agent to )
    {
        _from = from;
        _to = to;
        return this;
    }
    
    public EventGenerator setWaitReponse( final boolean flag )
    {
        _waitResponse = flag;
        return this;
    }
    
    public boolean waitForResponse() {
        return _waitResponse;
    }
    
    /**
     * Update the internal state of the generator.</br>
     * This function is called everytime a new event arrived.
    */
    public abstract void update();
    
    /**
     * Generate a new event.</br>
     * NOTE: event can be null.
     * 
     * @param t    time of the simulator
     * @param e    input event
    */
    public Event generate( final Time t, final Event e )
    {
        //System.out.println( "MY_TIME: " + _time + ", INPUT_TIME: " + t );
        if (t != null && waitForResponse())
            _time = t;
        
        _time.addTime( _departureTime );
        if(_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        //System.out.println( "EVENT: " + e );
        if (e instanceof ResponseEvent)
            update();
        
        Event event = null;
        if (e instanceof RequestEvent) {
            event = new ResponseEvent( _time.clone(), e._to, e._from, _resPacket );
        } else {
            if (_packetsInFly < _maxPacketsInFly) {
                _packetsInFly = (_packetsInFly + 1L) % SimulatorUtils.INFINITE;
                event = new RequestEvent( _time.clone(), _from, _to, _reqPacket );
            }
        }
        
        return event;
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public boolean isActive() {
        return _activeGenerator;
    }
    
    



    // TODO Magari metterle come "specializzazioni" di generatori di eventi.
    /*public static abstract class ConstantEventGenerator extends EventGenerator
    {
        
        public ConstantEventGenerator( final Time duration,
                                       final Time departureTime,
                                       final Packet pktSize )
        {
            super( duration, departureTime, pktSize );
            setWaitReponse( false );
        }
        
        @Override
        public void update() {}
    }
    
    // TODO Gestire in maniera efficace anche questo generatore:
    // TODO prima di tutto devo inserire una variabile che conti il numero di pacchetti in volo
    // TODO se il numero e' minore di una certa soglia allora spedisco il prossimo messaggio,
    // TODO altrimenti aspetto una risposta per almeno uno di essi prima di andare avanti.
    public static abstract class ResponseEventGenerator extends EventGenerator
    {
        
        public ResponseEventGenerator( final Time duration,
                                       final Packet pktSize )
        {
            super( duration, Time.ZERO, 1L, pktSize );
            setWaitReponse( true );
        }
    }*/
}