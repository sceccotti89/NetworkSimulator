/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.ArrayList;
import java.util.List;
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
    protected List<Agent> _destinations;
    
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
        
        _destinations = new ArrayList<>();
    }
    
    public void setAgent( final Agent from ) {
        _from = from;
    }
    
    public EventGenerator connect( final Agent to )
    {
        _destinations.add( to );
        return this;
    }
    
    public EventGenerator connect( final List<Agent> to )
    {
        _destinations.addAll( to );
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
     * The default method reduce by 1 the number of flying packets, but the user can
     * extend it to properly update the generator.</br>
     * NOTE: this method is called everytime a new event arrived.</br>
    */
    public void update() {
        _packetsInFly--;
    }
    
    /**
     * Generate a new packet to be sent.</br>
     * This method is called only if the request or response packet is
     * {@link simulator.Packet#DYNAMIC DYNAMIC}.</br>
     * A typical usage of the input event {@code e} is:
     * 
     * <pre>
     * if (e instanceof RequestEvent) {
     *     ... // produce response packet
     * } else {
     *     ... // produce request packet
     * }
     * </pre>
     * 
     * @param e    the input event
     * 
     * @return the generated packet.</br>
     * NOTE: the returned packet must NOT be null.
    */
    public abstract Packet makePacket( final Event e );
    
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
        if (_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        //System.out.println( "EVENT: " + e );
        if (e instanceof ResponseEvent)
            update();
        
        Event event = null;
        if (e instanceof RequestEvent) {
            // Prepare the response packet.
            Packet resPacket = _resPacket;
            if (_resPacket.isDynamic())
                resPacket = makePacket( e );
            
            event = new ResponseEvent( _time.clone(), e._to, e._from, resPacket );
        } else {
            if (_packetsInFly < _maxPacketsInFly) {
                _packetsInFly = (_packetsInFly + 1L) % SimulatorUtils.INFINITE;
                
                // Prepare the request packet.
                Packet reqPacket = _reqPacket;
                if (_resPacket.isDynamic())
                    reqPacket = makePacket( e );
                
                for (Agent dest : _destinations) {
                    event = new RequestEvent( _time.clone(), _from, dest, reqPacket );
                }
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
    
    



    // TODO Magari metterle come "specializzazioni" di generatori di eventi, ma per adesso sono inutili.
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