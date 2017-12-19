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
    
    private long _forwardFrom = -1;
    
    private boolean _isBroadcasted = false;
    
    private long _maxPacketsInFlight = 0;
    private boolean _setByTheUser    = false;
    
    private Map<Long,Session> _sessions;
    private Session _session;
    
    // Dummy packet used to generate the corresponding request event.
    private ResponseEvent dummyResEvent;
    
    private long _id;
    
    private static long nextID = -1;
    
    
    /**
     * Creates a new event generator.
     * 
     * @param duration              lifetime of the generator.
     * @param departureTime         time to wait before sending a packet.
     * @param reqPacket             the request packet.
     * @param resPacket             the response packet.
    */
    public EventGenerator( Time duration,
                           Time departureTime,
                           Packet reqPacket,
                           Packet resPacket )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _duration           = duration;
        _reqPacket          = reqPacket;
        _resPacket          = resPacket;
        _departureTime      = departureTime;
        
        _sessions = new HashMap<>();
        _destinations = new ArrayList<>();
    }
    
    public long getId() {
        return _id;
    }
    
    public void setAgent( Agent agent )
    {
        _id = getNextID();
        _agent = agent;
        dummyResEvent = new ResponseEvent( Time.ZERO, _agent, null, null );
        dummyResEvent.setGeneratorID( _id );
    }
    
    public Agent getAgent() {
        return _agent;
    }
    
    public EventGenerator connect( Agent to )
    {
        _destinations.add( to );
        if (!_setByTheUser) {
            _maxPacketsInFlight++;
        }
        return this;
    }
    
    public EventGenerator connectAll( List<Agent> to )
    {
        for (Agent dest : _destinations) {
            connect( dest );
        }
        return this;
    }
    
    /**
     * Sets the generator as a broadcast.
    */
    public void setBroadcast( boolean flag ) {
        _isBroadcasted = flag;
    }
    
    /**
     * Sets the time when the generator starts at.</br>
     * This method lets the generator to send events in any moment.
     * 
     * @param time    time of start
    */
    public void startAt( Time time )
    {
        _activeGenerator = true;
        _time.setTime( time );
        //_maxPacketsInFlight = Utils.INFINITE;
        _maxPacketsInFlight = Math.max( _maxPacketsInFlight, 1 );
        _setByTheUser = true;
        setWaitForResponse( false );
    }
    
    /**
     * Sets the maximum number of packets in flight.</br>
     * The minimum number of packets is internally sets as the maximum between 1 and the given value.</br>
     * In addition forces the generator to wait for the response before sending new packets.
    */
    public void setMaximumFlyingPackets( long packets )
    {
        _maxPacketsInFlight = Math.max( packets, 1 );
        _setByTheUser = true;
        setWaitForResponse( true );
    }
    
    /**
     * Sets this generator to wait the response of any sent message before sending the next one.
     * 
     * @param flag    {@code true} to wait for a response, {@code false} otherwise.
    */
    public void setWaitForResponse( boolean flag ) {
        _waitResponse = flag;
    }
    
    /**
     * Sets this generator to answer the source after the reception of a message
     * as a reponse of an out-going packet.
     * 
     * @param flag    {@code true} to answer after a reponse of an out-going packet,
     *                {@code false} to send it immediately.
    */
    public void setDelayedResponse( boolean flag ) {
        _delayResponse = flag;
    }
    
    public List<Agent> getDestinations() {
        return _destinations;
    }
    
    public EventGenerator setWaitReponse( boolean flag )
    {
        _waitResponse = flag;
        return this;
    }
    
    public EventGenerator makeAnswer( boolean flag ) {
        _makeAnswer = flag;
        return this;
    }
    
    /**
     * Forwards messages coming from the given node, represented by its agent.</br>
     * This means that a reply for those incoming messages is not performed.</br>
     * This is different to a delayed response, since this method does not provide a response
     * for the specified node.
     * 
     * @param from    the agent of the node
     *
     * @see EventGenerator#setDelayedResponse(boolean)
    */
    public EventGenerator forwardMessagesFrom( Agent from ) {
        return forwardMessagesFrom( from.getId() );
    }
    
    /**
     * Forwards messages coming from the given node identifier.</br>
     * This means that a reply for those incoming messages is not performed.</br>
     * This is different to a delayed response, since this method does not provide a response
     * for the specified node.
     * 
     * @param id    the given node identifer
     *
     * @see EventGenerator#setDelayedResponse(boolean)
    */
    public EventGenerator forwardMessagesFrom( long id ) {
        _forwardFrom = id;
        return this;
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    private void setTime( Time t ) {
        _time = t;
    }
    
    protected Packet getRequestPacket() {
        return _reqPacket.clone();
    }
    
    protected Packet getResponsePacket() {
        return _resPacket.clone();
    }
    
    /**
     * Generates a new packet to sent to the destination node, expressed by its identifier.</br>
     * In case of broadcast operation the destination assumes value {@code -1},
     * and this method is called once for all the possible destinations.</br>
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
    protected Packet makePacket( Event e, long destination )
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
    
    public void setDepartureTime( Time time ) {
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
    protected Time computeDepartureTime( Event e ) {
        return _departureTime;
    }
    
    /**
     * Retrieves the session associated with the given event.
     * 
     * @param e    the given event.
    */
    private Session getSession( Event e )
    {
        Session session;
        if (e == null || _activeGenerator && !_waitResponse) {
            session = new Session();
            session.setMaximumFlyingPackets( _maxPacketsInFlight );
            _sessions.put( session.getId(), session );
        } else {
            if ((session = _sessions.get( e.getFlowId() )) == null) {
                session = new Session( e.getFlowId() );
                session.setSource( e.getSource() );
                session.setMaximumFlyingPackets( _maxPacketsInFlight );
                _sessions.put( session.getId(), session );
            }
        }
        
        return session;
    }
    
    /**
     * Checks whether a new event can be generated.
     * 
     * @param e    the given event.
    */
    private boolean generateEvent( Event e )
    {
        if (_time.compareTo( _duration ) > 0) {
            return false;
        }
        if (e == null && !_activeGenerator) {
            return false;
        }
        
        return true;
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
    public final Event generate( Time t, Event e )
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
        
        // FIXME ora come ora i generatori attivi perdono il tempo di invio del
        // FIXME precedente evento, cioe' e' in ritardo di 1.
        // FIXME al fine del simulatore cambia poco ma dovrei trovare un modo per sistemarlo.
        if (!_activeGenerator) {
            _time.addTime( departureTime );
        }
        if (_time.compareTo( _duration ) > 0) {
            return null; // No more events from this generator.
        }
        
        // Load the current session.
        _session = getSession( e );
        return createMessage( _session, e, departureTime );
    }
    
    protected Event createMessage( Session session, Event e, Time departureTime )
    {
        if (_activeGenerator && _session.canSend()) {
            dummyResEvent.setTime( _time );
            Event request = sendRequest( dummyResEvent );
            _time.addTime( departureTime );
            return request;
        }
        
        Event event = null;
        if (e instanceof RequestEvent) {
            if (e.getSource().getId() == _agent.getId()) {
                if (_session.canSend()) {
                    dummyResEvent.setTime( _time );
                    dummyResEvent.setPacket( _session.getPacket() );
                    event = sendRequest( dummyResEvent );
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
                    // Received a request message from this node, just forward it to the next nodes.
                    if (e.getSource().getId() == _forwardFrom) {
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
            }
        } else {
            // Response message.
            if (e.getSource().getId() != _agent.getId()) {
                _session.increaseReceivedPackets();
                if (_delayResponse) {
                    if (_session.completed()) {
                        // Send back the "delayed" response.
                        Agent dest = _session.getSource();
                        event = sendResponse( e, _agent, dest );
                        checkCompletedSession( e );
                    }
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
     * Cehcks whether the current session is over.
    */
    private void checkCompletedSession( Event e )
    {
        if (_session.completed()) {
            _sessions.remove( _session.getId() );
            _session = getSession( e );
        }
    }

    /**
     * Generates a new request messages.
     * 
     * @param e    the current received event.
     *             It can be {@code null}.
     * 
     * @return the request messages.
    */
    private Event sendRequest( Event e )
    {
        Event event = null;
        int nextDest = selectDestination( _time );
        Agent dest = _destinations.get( nextDest );
        
        // Get the request packet.
        Packet reqPacket = _session.getPacket();
        if (reqPacket == null || !_isBroadcasted) {
            reqPacket = makePacket( e, (_isBroadcasted) ? -1 : dest.getId() );
        }
        
        if (reqPacket != null) {
            _session.increaseSentPackets();
            event = new RequestEvent( _time, _agent, dest, reqPacket.clone() );
            event.setFlowId( _session.getId() );
            event.setGeneratorID( getId() );
        }
        
        return event;
    }
    
    /**
     * Select the next destination node at the specified time.</br>
     * By default it returns the next available node, in a Round-Robin fashion.
     * 
     * @param time    time to select the destination. Useful in multicast/anycast transmissions
     *                where, for instance, the destination node is selected based on
     *                the workload at the given time.
     * 
     * @return Index of the next destination.</br>
     *         It must be in the range <b>[0 - #destinations)</b>
    */
    protected int selectDestination( Time time ) {
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
    private Event sendResponse( Event e, Agent from, Agent dest )
    {
        if (!_makeAnswer) {
            return null;
        }
        
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
            response.setGeneratorID( getId() );
            return response;
        }
    }
    
    private static synchronized long getNextID() {
        return ++nextID % Long.MAX_VALUE;
    }
}
