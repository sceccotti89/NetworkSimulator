/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.EventHandler.EventType;
import simulator.events.Packet;
import simulator.events.impl.RequestEvent;
import simulator.events.impl.ResponseEvent;
import simulator.utils.Time;
import simulator.utils.Utils;

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
    protected boolean _makeAnswer = true;
    protected Time    _departureTime;
    protected long    _packetsInFly = 0;
    protected long    _initMaxPacketsInFlight;
    protected long    _maxPacketsInFlight = 0;
    
    // Used for multiple destinations.
    private boolean _isMulticasted = false;
    private boolean _optimizedMulticast = false;
    private int     _nextDestIndex = -1;
    private boolean _continueToSend = false;
    protected int   _destIndex = 0;
    protected List<Agent> _toAnswer;
    
    
    
    /**
     * Creates a new event generator.
     * 
     * @param duration              time (in ms) of life of the generator.
     * @param departureTime         time (in ms) to wait before sending a packet.
     * @param maxPacketsInFlight    maximum number of packets in flight.
     * @param reqPacket             the request packet.
     * @param resPacket             the response packet.
     * @param isActive              {@code TRUE} let this generator to send events in any moment, {@code FALSE} otherwise.
     * @param delayResponse         {@code TRUE} if the answer to the source is sent after the reception of a message as a reponse of an outgoing packet, {@code FALSE} to send it immediately.
     * @param waitResponse          {@code TRUE} let this generator to wait the response of any sent message before sending the next one, {@code FALSE} otherwise.
    */
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final long maxPacketsInFlight,
                           final Packet reqPacket,
                           final Packet resPacket,
                           final boolean isActive,
                           final boolean delayResponse,
                           final boolean waitResponse )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration           = duration;
        _maxPacketsInFlight = maxPacketsInFlight;
        _reqPacket          = reqPacket;
        _resPacket          = resPacket;
        _departureTime      = departureTime;
        _activeGenerator    = isActive;
        _delayResponse      = delayResponse;
        _waitResponse       = waitResponse;
        
        _initMaxPacketsInFlight = _maxPacketsInFlight;
        
        _destinations = new ArrayList<>();
        
        _toAnswer = new LinkedList<>();
        
        // TODO assegnare i protocolli di comunicazione ad ogni livello (crearne anche il metodo).
        
    }
    
    /**
     * Set the communication as a multicast.</br>
     * This is usefull in case of sending several copies of the input message to multiple destinations.</br>
     * The {@linkplain #selectDestination()} method could be overrided to generate a proper next destination node.
     * 
     * @param multicasted    {@code true} if the output transmission is considered as a multicast,
     *                       {@code false} otherwise
     * @param optimized      {@code true} if the multicast is optimized, hence the Ttrasm is not payed among two consecutive messages,
     *                       {@code false} otherwise
    */
    public EventGenerator setMulticast( final boolean multicasted, final boolean optimized )
    {
        _isMulticasted = multicasted;
        if (multicasted) {
            _maxPacketsInFlight = _initMaxPacketsInFlight * _destinations.size();
            _optimizedMulticast = optimized;
        }
        return this;
    }
    
    public void setAgent( final Agent agent ) {
        _agent = agent;
    }
    
    public EventGenerator connect( final Agent to )
    {
        _destinations.add( to );
        if (_isMulticasted) {
            _maxPacketsInFlight = _initMaxPacketsInFlight * _destinations.size();
        }
        return this;
    }
    
    public EventGenerator connectAll( final List<Agent> to ) {
        for (Agent dest : _destinations) {
            connect( dest );
        }
        return this;
    }
    
    public List<Agent> getDestinations() {
        return _destinations;
    }
    
    public EventGenerator setWaitReponse( final boolean flag )
    {
        _waitResponse = flag;
        return this;
    }
    
    public EventGenerator makeAnswer( final boolean flag ) {
        _makeAnswer = flag;
        return this;
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public void setTime( final Time t ) {
        _time = t;
    }
    
    public boolean isActive() {
        return _activeGenerator;
    }
    
    /**
     * Update the internal state of the generator.</br>
     * This method is called everytime a new event arrive.</br>
     * By default it reduces by 1 the number of flying packets, but it can</br>
     * extended to properly update the event generator.</br>
    */
    public void update() {
        _packetsInFly--;
    }
    
    public Packet getRequestPacket() {
        return _reqPacket.clone();
    }
    
    public Packet getResponsePacket() {
        return _resPacket.clone();
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
     * {@link simulator.utils.Time#DYNAMIC DYNAMIC}.</br>
     * In case of fixed interval just return {@code null}.</br>
     * Returning {@code null} with dynamic time makes the generator to produce no event.
     * 
     * @param e    the input event
     * 
     * @return the departure time.</br>
     * NOTE: returned time can be {@code null}.
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
    public List<Event> generate( final Time t, final Event e )
    {
        if (!_makeAnswer) {
            return null;
        }
        
        if (t != null && (_waitResponse || _delayResponse)) {
            setTime( t );
        }
        
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
        if (e instanceof RequestEvent) {
            if (_delayResponse) {
                // Prepare and send the new request packet to the next node.
                Event event = new ResponseEvent( e.getArrivalTime(), _agent, null, e.getPacket() );
                event.setArrivalTime( e.getArrivalTime() );
                events = sendRequest( event );
                _toAnswer.add( e.getSource() );
            } else {
                events = sendResponse( e, e.getDestination(), e.getSource() );
            }
        } else {
            update();
            // Response message.
            if (e != null && !_toAnswer.isEmpty()) {
                if (!_isMulticasted) {
                    Agent dest = _toAnswer.remove( 0 );
                    events = sendResponse( e, _agent, dest );
                } else {
                    if (++_destIndex == _destinations.size()) {
                        // Send back the "delayed" response.
                        Agent dest = _toAnswer.remove( 0 );
                        events = sendResponse( e, _agent, dest );
                        _destIndex = 0;
                    }
                }
            } else {
                if (_activeGenerator || _continueToSend) {
                    Event event = new ResponseEvent( _time, _agent, null, null );
                    event.setArrivalTime( _time );
                    events = sendRequest( event );
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
        if (_packetsInFly < _maxPacketsInFlight) {
            // Prepare the request packet.
            Packet reqPacket = makePacket( e );
            if (reqPacket != null) {
                if (_optimizedMulticast) {
                    _packetsInFly = (_packetsInFly + _destinations.size()) % Utils.INFINITE;
                    events = new ArrayList<>( _destinations.size() );
                    for (int i = 0; i < _destinations.size(); i++) {
                        Agent dest = _destinations.get( _nextDestIndex = selectDestination( e.getArrivalTime() ) );
                        Event request = new RequestEvent( _time.clone(), _agent, dest, reqPacket.clone() );
                        request.setArrivalTime( e.getArrivalTime() );
                        events.add( request );
                    }
                } else {
                    _packetsInFly = (_packetsInFly + 1) % Utils.INFINITE;
                    Agent dest = _destinations.get( _nextDestIndex = selectDestination( e.getArrivalTime() ) );
                    Event request = new RequestEvent( _time.clone(), _agent, dest, reqPacket.clone() );
                    events = Collections.singletonList( request );
                    _continueToSend = _isMulticasted && _packetsInFly < _maxPacketsInFlight;
                }
            }
        }
        
        return events;
    }
    
    /**
     * Select the index of the next destination node at the specified time.
     * 
     * @param time    time to check the destination. Useful in multicast/anycast transmissions
     *                where, for instance, the destination node is selected based on their workloads.
     * 
     * @return Index of the next destination.</br>
     *         It must be in the range <b>[0 - #destinations)</b>
    */
    protected int selectDestination( final Time time ) {
        return (_nextDestIndex + 1) % _destinations.size();
    }
    
    /**
     * Sends a new list of response messages.
     * 
     * @param e     the current received event.
     * @param from  the source node
     * @param dest  the destination node
     * 
     * @return the list of response messages
    */
    private List<Event> sendResponse( final Event e, final Agent from, final Agent dest )
    {
        Packet resPacket = makePacket( e );
        
        if (resPacket == null) {
            return null;
        } else {
            Event response = new ResponseEvent( _time.clone(), from, dest, resPacket );
            if (from.getEventHandler() != null) {
                from.getEventHandler().handle( response, EventType.GENERATED );
            }
            response.setArrivalTime( e.getArrivalTime() );
            return Collections.singletonList( response );
        }
    }
}