/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    
    private   Time _duration;
    private   Packet _reqPacket;
    private   Packet _resPacket;
    
    private   Agent _agent;
    protected List<Agent> _destinations;
    
    private boolean _activeGenerator = false;
    private boolean _waitResponse = true;
    private boolean _delayResponse = false;
    private boolean _makeAnswer = true;
    private Time    _departureTime;
    
    private boolean _isMulticasted = false;
    
    private long _maxPacketsInFlight = 0;
    private boolean _setByTheUser    = false;
    
    private Map<Long,FlowSession> _sessions;
    private FlowSession _session;
    
    // Dummy packet used to generate the corresponding request event.
    private ResponseEvent dummyResEvent;
    
    
    // TODO Molti dei valori nel costruttore andranno tolti o messi in appositi metodi:
    /**
     * Creates a new event generator.
     * 
     * @param duration              lifetime of the generator.
     * @param departureTime         time to wait before sending a packet.
     * @param reqPacket             the request packet.
     * @param resPacket             the response packet.
     * @param delayResponse         {@code TRUE} to answer the source after the reception of a message as a reponse of an out-going packet,
     *                              {@code FALSE} to send it immediately.
    */
    public EventGenerator( final Time duration,
                           final Time departureTime,
                           final Packet reqPacket,
                           final Packet resPacket,
                           final boolean delayResponse )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration           = duration;
        _reqPacket          = reqPacket;
        _resPacket          = resPacket;
        _departureTime      = departureTime;
        _delayResponse      = delayResponse;
        
        _sessions = new HashMap<>();
        _destinations = new ArrayList<>();
        
        // TODO assegnare i protocolli di comunicazione ad ogni livello (crearne anche il metodo).
        
    }
    
    public void setAgent( final Agent agent )
    {
        _agent = agent;
        dummyResEvent = new ResponseEvent( Time.ZERO, _agent, null, null );
    }
    
    public EventGenerator connect( final Agent to )
    {
        _destinations.add( to );
        if (!_setByTheUser) {
            _maxPacketsInFlight++;
        }
        return this;
    }
    
    public EventGenerator connectAll( final List<Agent> to ) {
        for (Agent dest : _destinations) {
            connect( dest );
        }
        return this;
    }
    
    /**
     * 
    */
    public void setMulticast( final boolean flag ) {
        _isMulticasted = flag;
    }
    
    /**
     * Sets the time when the generator starts at.</br>
     * This method lets the generator to send events in any moment.
     * 
     * @param time    time of start
    */
    public void startAt( final Time time )
    {
        _activeGenerator = true;
        _time.setTime( time );
        _maxPacketsInFlight = Utils.INFINITE;
        _setByTheUser = true;
        setWaitForResponse( false );
    }
    
    /**
     * Sets the maximum number of packets in flight.</br>
     * The minimum number of packets is internally sets as the maximum between 1 and the given value.</br>
     * In addition forces the generator to wait for the response before sending new packets.
    */
    public void setMaximumFlyingPackets( final long packets )
    {
        _maxPacketsInFlight = Math.max( packets, 1 );
        _setByTheUser = true;
        setWaitForResponse( true );
    }
    
    /**
     * Sets this generator to wait the response of any sent message before sending the next one.
     * 
     * @param flag    {@code TRUE} , {@code FALSE} otherwise.
    */
    public void setWaitForResponse( final boolean flag ) {
        _waitResponse = flag;
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
    
    private void setTime( final Time t ) {
        _time = t;
    }
    
    /***/
    public Packet getRequestPacket() {
        return _reqPacket.clone();
    }
    
    /***/
    public Packet getResponsePacket() {
        return _resPacket.clone();
    }
    
    /**
     * Generates a new packet to be sent.</br>
     * In case of multicast operation the destination assumes value {@code -1}
     * and this method is called only once for all the possible destinations.</br>
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
     * @param e              the input event.
     * @param destination    the destination node identifier.
     * 
     * @return the generated packet.</br>
     * NOTE: the returned packet can be {@code null}.
    */
    protected Packet makePacket( final Event e, final long destination )
    {
        if (e instanceof RequestEvent) {
            return getResponsePacket();
        } else {
            return getRequestPacket();
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
     * Returning {@code null} makes the generator to produce no event.
     * 
     * @param e    the input event
     * 
     * @return the departure time.</br>
     * NOTE: returned time can be {@code null}.
    */
    protected Time computeDepartureTime( final Event e ) {
        return _departureTime;
    }
    
    /**
     * Retrieves the session associated with the given event.
     * 
     * @param e    the given event.
    */
    private FlowSession getSession( final Event e )
    {
        FlowSession session;
        if (e == null) {
            session = new FlowSession();
            session.setMaximumFlyingPackets( _maxPacketsInFlight );
            _sessions.put( session.getId(), session );
        } else {
            if (_activeGenerator && !_waitResponse) {
                session = new FlowSession();
                session.setMaximumFlyingPackets( _maxPacketsInFlight );
                _sessions.put( session.getId(), session );
            } else {
                if ((session = _sessions.get( e.getFlowId() )) == null) {
                    session = new FlowSession( e.getFlowId() );
                    session.setSource( e.getSource() );
                    session.setMaximumFlyingPackets( _maxPacketsInFlight );
                    _sessions.put( session.getId(), session );
                }
            }
        }
        
        return session;
    }
    
    /**
     * Checks whether a new event can be generated.
     * 
     * @param e    the given event.
    */
    private boolean generateEvent( final Event e )
    {
        if (!_makeAnswer || _time.compareTo( _duration ) > 0) {
            return false;
        }
        if (e == null && !_activeGenerator) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Cehcks whether the current session is over.
    */
    private void checkCompletedSession( final Event e )
    {
        if (_session.completed()) {
            _sessions.remove( _session.getId() );
            _session = getSession( e );
        }
    }
    
    /**
     * Generates a new event.</br>
     * NOTE: input time and event can be {@code null}.
     * 
     * @param t    time of the simulator
     * @param e    input event
     * 
     * @return the new event list, or {@code null} if the time is expired.
    */
    public final Event generate( final Time t, final Event e )
    {
        if (!generateEvent( e )) {
            return null;
        }
        
        if (t != null && (_waitResponse || _delayResponse)) {
            setTime( t );
        }
        
        Time departureTime = computeDepartureTime( e );
        if (departureTime == null)
            return null; // No events from this generator.
        
        _time.addTime( departureTime );
        if (_time.compareTo( _duration ) > 0)
            return null; // No more events from this generator.
        
        // Load the current session.
        _session = getSession( e );
        
        if (e == null && _activeGenerator && _session.canSend()) {
            dummyResEvent.setTime( _time );
            return sendRequest( dummyResEvent );
        }
        
        Event event = null;
        if (e instanceof RequestEvent) {
            if (e.getSource().getId() == _agent.getId()) {
                if (_session.canSend()) {
                    dummyResEvent.setTime( _time );
                    dummyResEvent.setPacket( _session.getPacket() );
                    return sendRequest( dummyResEvent );
                }
            } else {
                if (_delayResponse) {
                    // Prepare and send the new request packet to the next node.
                    if (_session.canSend()) {
                        dummyResEvent.setTime( _time );
                        dummyResEvent.setPacket( e.getPacket() );
                        _session.setPacket( e.getPacket() );
                        event = sendRequest( dummyResEvent );
                    }
                } else {
                    event = sendResponse( e, e.getDestination(), e.getSource() );
                }
            }
        } else {
            // Response message.
            if (e.getSource().getId() != _agent.getId()) {
                _session.increaseReceivedPackets();
                if (_delayResponse) {
                    // Send back the "delayed" response.
                    Agent dest = _session.getSource();
                    event = sendResponse( e, _agent, dest );
                    checkCompletedSession( e );
                } else {
                    checkCompletedSession( e );
                    if (_session.canSend()) {
                        dummyResEvent.setTime( _time );
                        event = sendRequest( dummyResEvent );
                    }
                }
            }
        }
        
        return event;
    }
    
    /**
     * Generates a new request messages.
     * 
     * @param e    the current received event. It can be {@code null}.
     * 
     * @return the request messages.
    */
    private Event sendRequest( final Event e )
    {
        Event event = null;
        int nextDest = selectDestination( _time );
        Agent dest = _destinations.get( nextDest );
        
        // Get the request packet.
        Packet reqPacket = _session.getPacket();
        if (reqPacket == null || !_isMulticasted) {
            reqPacket = makePacket( e, (_isMulticasted) ? -1 : dest.getId() );
        }
        if (reqPacket != null) {
            _session.increaseSentPackets();
            event = new RequestEvent( _time, _agent, dest, reqPacket.clone() );
            event.setFlowId( _session.getId() );
        }
        
        return event;
    }
    
    /**
     * Select the next destination node at the specified time.
     * 
     * @param time    time to check the destination. Useful in multicast/anycast transmissions
     *                where, for instance, the destination node is selected based on its workload.
     * 
     * @return Index of the next destination.</br>
     *         It must be in the range <b>[0 - #destinations)</b>
    */
    protected int selectDestination( final Time time ) {
        return _session.getNextDestination( _destinations.size() );
    }
    
    /**
     * Generates a new list of response messages.
     * 
     * @param e       the received event.
     * @param from    the source node
     * @param dest    the destination node
     * 
     * @return the list of response messages.
    */
    private Event sendResponse( final Event e, final Agent from, final Agent dest )
    {
        Packet resPacket = makePacket( e, dest.getId() );
        //Packet resPacket = makePacket( e, session.getSource().getId() );
        
        if (resPacket == null) {
            return null;
        } else {
            Event response = new ResponseEvent( _time, from, dest, resPacket );
            response.setFlowId( _session.getId() );
            if (from.getEventHandler() != null) {
                from.getEventHandler().handle( response, EventType.GENERATED );
            }
            return response;
        }
    }
}