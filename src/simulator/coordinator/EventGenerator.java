/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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
    protected boolean _delayResponse = false;
    protected long _packetsInFly = 0;
    protected long _maxPacketsInFly = 0;
    
    protected int _destIndex = 0;
    protected List<Agent> _toAnswer;
    
    
    
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final long maxPacketsInFly,
                           final Packet reqPacket,
                           final Packet resPacket,
                           final boolean isActive,
                           final boolean delayResponse,
                           final boolean waitResponse )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration        = duration;
        _departureTime   = departureTime;
        _maxPacketsInFly = maxPacketsInFly;
        _reqPacket       = reqPacket;
        _resPacket       = resPacket;
        _activeGenerator = isActive;
        _delayResponse   = delayResponse;
        _waitResponse    = waitResponse;
        
        _destinations = new ArrayList<>();
        
        _toAnswer = new LinkedList<>();
    }
    
    public void setAgent( final Agent from ) {
        _from = from;
    }
    
    public EventGenerator connect( final Agent to )
    {
        _destinations.add( to );
        _maxPacketsInFly += _destinations.size() - 1;
        return this;
    }
    
    public EventGenerator connectAll( final List<Agent> to )
    {
        for (Agent dest : _destinations)
            connect( dest );
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
     * This method is called everytime a new event arrive.</br>
     * By default it reduces by 1 the number of flying packets, but the user can
     * extend it to properly update the generator.</br>
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
     * Generate a new list of events.</br>
     * NOTE: time and event can be null.
     * 
     * @param t    time of the simulator
     * @param e    input event
     * 
     * @return the new events list, or {@code null} if the time is expired.
    */
    public List<Event> generate( final Time t, final Event e )
    {
        //System.out.println( "MY_TIME: " + _time + ", INPUT_TIME: " + t );
        if (t != null && waitForResponse())
            _time = t;
        
        _time.addTime( _departureTime );
        if (_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        List<Event> events = null;
        
        //System.out.println( "EVENT: " + e );
        if (e instanceof ResponseEvent) {
            //System.out.println( "ID: " + _from.getId() + ", RICEVUTA EVENTO RESPONSE: " + e );
            update();
        }
        
        if (e instanceof RequestEvent) {
            if (_delayResponse/* && !fromDestinationNode( e.getSource().getId() )*/) {
                // Prepare and send the new request packet to the next node.
                events = sendRequest( new ResponseEvent( _time.clone(), _from, null, null ) );
                _toAnswer.add( e.getSource() );
            } else {
                //System.out.println( "ID: " + _from.getId() + ", RICEVUTA RICHIESTA: " + e );
                events = sendResponse( e, e.getDest(), e.getSource() );
            }
        } else {
            //System.out.println( "ID: " + _from.getId() + ", RICEVUTA RESPONSE: " + e );
            if (e != null && !_toAnswer.isEmpty()) {
                if (++_destIndex == _destinations.size()) {
                    Agent dest = _toAnswer.remove( 0 );
                    events = sendResponse( e, _from, dest );
                    _destIndex = 0;
                }
            } else {
                if (_activeGenerator)
                    events = sendRequest( e );
            }
        }
        
        return events;
    }
    
    /***/
    private List<Event> sendRequest( final Event e )
    {
        List<Event> events = null;
        if (_packetsInFly < _maxPacketsInFly) {
            _packetsInFly = (_packetsInFly + _destinations.size()) % SimulatorUtils.INFINITE;
            //System.out.println( "TIME: " + _time + ", ID: " + _from.getId() + ", CREATO EVENTO!" );
            
            // Prepare the request packet.
            Packet reqPacket = _reqPacket;
            if (_resPacket.isDynamic())
                reqPacket = makePacket( e );
            
            // TODO chiedere se quasto for puo' andare bene, o se bisogna inviare un messaggio alla volta.
            events = new ArrayList<>( _destinations.size() );
            for (Agent dest : _destinations) {
                events.add( new RequestEvent( _time.clone(), _from, dest, reqPacket.clone() ) );
            }
        }
        
        return events;
    }
    
    /***/
    private List<Event> sendResponse( final Event e, final Agent from, final Agent dest )
    {
        Packet resPacket = _resPacket;
        if (_resPacket.isDynamic())
            resPacket = makePacket( e );
        
        return Collections.singletonList( new ResponseEvent( _time.clone(), from, dest, resPacket.clone() ) );
    }
    
    /***/
    /*private boolean fromDestinationNode( final long id )
    {
        for (Agent dest : _destinations) {
            if (dest.getId() == id)
                return true;
        }
        return false;
    }*/
    
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