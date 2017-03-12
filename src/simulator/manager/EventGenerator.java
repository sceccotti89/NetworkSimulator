/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.Packet;
import simulator.core.Time;
import simulator.manager.Event.RequestEvent;
import simulator.manager.Event.ResponseEvent;
import simulator.utils.SimulatorUtils;

public abstract class EventGenerator
{
    protected Time _time;
    
    protected Time _duration;
    protected Packet _reqPacket;
    protected Packet _resPacket;
    
    protected Agent _agent;
    protected List<Agent> _destinations;
    
    protected boolean _activeGenerator = false;
    protected boolean _waitResponse = true;
    protected boolean _delayResponse = false;
    protected Time    _departureTime;
    protected long    _packetsInFly = 0;
    protected long    _initMaxPacketsInFly;
    protected long    _maxPacketsInFly = 0;
    
    private boolean _optimizedMulticast = false;
    private int     _nextDestIndex = 0;
    private boolean _continueToSend = false;
    
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
        _maxPacketsInFly = maxPacketsInFly;
        _reqPacket       = reqPacket;
        _resPacket       = resPacket;
        _departureTime   = departureTime;
        _activeGenerator = isActive;
        _delayResponse   = delayResponse;
        _waitResponse    = waitResponse;
        
        _initMaxPacketsInFly = _maxPacketsInFly;
        
        _destinations = new ArrayList<>();
        
        _toAnswer = new LinkedList<>();
    }
    
    public EventGenerator setOptimizedMulticast( final boolean optimized ) {
        _optimizedMulticast = optimized;
        return this;
    }
    
    public void setAgent( final Agent agent ) {
        _agent = agent;
    }
    
    public EventGenerator connect( final Agent to )
    {
        _destinations.add( to );
        _maxPacketsInFly = _initMaxPacketsInFly * _destinations.size();
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
    
    public Time getTime() {
        return _time.clone();
    }
    
    public boolean isActive() {
        return _activeGenerator;
    }
    
    public boolean waitForResponse() {
        return _waitResponse;
    }
    
    /**
     * Update the internal state of the generator.</br>
     * This method is called everytime a new event arrive.</br>
     * By default it reduces by 1 the number of flying packets, but the user can</br>
     * extend it to properly update the event generator.</br>
    */
    public void update() {
        _packetsInFly--;
    }
    
    /**
     * Generate a new packet to be sent.</br>
     * User can override this method to create a proper custom packet.</br>
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
     * NOTE: the returned packet can be {@code null}.
    */
    public Packet makePacket( final Event e )
    {
        if (e instanceof RequestEvent) {
            return _resPacket.clone();
        } else {
            return _reqPacket.clone();
        }
    }
    
    public Time getDepartureTime() {
        return _departureTime.clone();
    }
    
    public void setDepartureTime( final Time time ) {
        _departureTime = time;
    }

    /**
     * Returns the departure time of the next event from this node.</br>
     * This method is called only if the specified departure time is
     * {@link simulator.core.Time#DYNAMIC DYNAMIC}.</br>
     * In case of fixed interval just return {@code null}.
     * 
     * @param e    the input event
     * 
     * @return the departure time.</br>
     * NOTE: time can be {@code null}.
    */
    public abstract Time computeDepartureTime( final Event e );
    
    /**
     * Generate a new list of events.</br>
     * NOTE: time and event can be {@code null}.
     * 
     * @param t    time of the simulator
     * @param e    input event
     * 
     * @return the new events list, or {@code null} if the time is expired.
    */
    final public List<Event> generate( final Time t, final Event e )
    {
        if (t != null && waitForResponse())
            _time = t;
        
        Time departureTime = _departureTime;
        if (_departureTime.isDynamic()) {
            departureTime = computeDepartureTime( e );
        }
        if (departureTime == null)
            return null; // No more events from this generator.
        
        _time.addTime( departureTime );
        if (_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        List<Event> events = null;
        
        if (e instanceof ResponseEvent)
            update();
        
        if (e instanceof RequestEvent) {
            if (_delayResponse) {
                // Prepare and send the new request packet to the next node.
                events = sendRequest( new ResponseEvent( null, _agent, null, null ) );
                _toAnswer.add( e.getSource() );
            } else {
                events = sendResponse( e, e.getDest(), e.getSource() );
            }
        } else {
            if (e != null && !_toAnswer.isEmpty()) {
                if (++_destIndex == _destinations.size()) {
                    Agent dest = _toAnswer.remove( 0 );
                    events = sendResponse( e, _agent, dest );
                    _destIndex = 0;
                }
            } else {
                if (_activeGenerator || _continueToSend) {
                    events = sendRequest( e );
                }
            }
        }
        
        return events;
    }
    
    /**
     * Sends a new list of request messages.
     * 
     * @param e    the current received event. It can be {@code null}.
     * 
     * @return the list of "sent" messages
    */
    private List<Event> sendRequest( final Event e )
    {
        List<Event> events = null;
        if (_packetsInFly < _maxPacketsInFly) {
            // Prepare the request packet.
            Packet reqPacket = makePacket( e );
            
            if (reqPacket != null) {
                if (_optimizedMulticast) {
                    _packetsInFly = (_packetsInFly + _destinations.size()) % SimulatorUtils.INFINITE;
                    events = new ArrayList<>( _destinations.size() );
                    for (Agent dest : _destinations) {
                        events.add( new RequestEvent( _time.clone(), _agent, dest, reqPacket.clone() ) );
                    }
                } else {
                    _packetsInFly = (_packetsInFly + 1) % SimulatorUtils.INFINITE;
                    
                    Agent dest = _destinations.get( _nextDestIndex );
                    Event request = new RequestEvent( _time.clone(), _agent, dest, reqPacket.clone() );
                    events = Collections.singletonList( request );
                    if (_packetsInFly % _initMaxPacketsInFly == 0) {
                        // Select the next destination node.
                        _nextDestIndex = (_nextDestIndex + 1) % _destinations.size();
                        _continueToSend = (_nextDestIndex != 0);
                    }
                }
            }
        }
        
        return events;
    }
    
    /**
     * Sends a new list of response messages.
     * 
     * @param e     the current received event. It could be {@code null}.
     * @param from  the source node
     * @param dest  the destination node
     * 
     * @return the list of respose messages
    */
    private List<Event> sendResponse( final Event e, final Agent from, final Agent dest )
    {
        Packet resPacket = makePacket( e );
        
        if (resPacket != null) {
            Event response = new ResponseEvent( _time.clone(), from, dest, resPacket );
            return Collections.singletonList( response );
        } else {
            return null;
        }
    }
}