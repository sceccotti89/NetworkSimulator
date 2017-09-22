/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import java.util.ArrayList;
import java.util.Collections;
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
    
    // TODO queste 2 variabili non sarebbero proprio necessarie
    // TODO perche' il numero massimo di pacchetti in volo (in genere) e' dato dal numero di destinatari.
    private long _initMaxPacketsInFlight;
    private long _maxPacketsInFlight = 0;
    
    // Used for multiple destinations.
    private boolean _isMulticasted = false;
    private FlowSession session;
    private Map<Long,FlowSession> _sessions;
    //private int     _nextDestIndex = -1;
    //private boolean _continueToSend = false;
    //protected int   _destIndex = 0;
    //protected List<Agent> _toAnswer;
    
    // Dummy packets used to generate the corresponding request.
    private RequestEvent  dummyReqEvent;
    private ResponseEvent dummyResEvent;
    
    
    // TODO Molti dei valori nel costruttore andranno tolti o messi in appositi metodi:
    // TODO isActive           - rimuoverlo e settarlo a FALSE: diventa TRUE solo se viene invocato il metodo "startAt( Time time )"
    // TODO maxPacketsInFlight - rimuoverlo
    /**
     * Creates a new event generator.
     * 
     * @param duration              lifetime of the generator.
     * @param departureTime         time to wait before sending a packet.
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
        
        _sessions = new HashMap<>();
        
        _destinations = new ArrayList<>();
        
        //_toAnswer = new LinkedList<>();
        
        // TODO assegnare i protocolli di comunicazione ad ogni livello (crearne anche il metodo).
        
    }
    
    /**
     * Set the communication as a multicast.</br>
     * This is usefull in case of sending several copies of the input message to multiple destinations.</br>
     * The {@linkplain #selectDestination()} method could be overridden to generate a proper next destination node.
     * 
     * @param multicasted    {@code true} if the output transmission is considered as a multicast,
     *                       {@code false} otherwise
    */
    public EventGenerator setMulticast( final boolean multicasted )
    {
        _isMulticasted = multicasted;
        /*if (multicasted) {
            _maxPacketsInFlight = _initMaxPacketsInFlight * _destinations.size();
            _optimizedMulticast = optimized;
        }*/
        return this;
    }
    
    public void setAgent( final Agent agent )
    {
        _agent = agent;
        dummyReqEvent = new RequestEvent( Time.ZERO, _agent, null, null );
        dummyResEvent = new ResponseEvent( Time.ZERO, _agent, null, null );
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
    
    /**
     * Sets the time when the generator starts at.</br>
     * In addition let the generator start sending events in an active mode.
     * 
     * @param time    time of start
    */
    public void startAt( final Time time ) {
        _activeGenerator = true;
        _time.setTime( time );
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
    
    public boolean isActive() {
        return _activeGenerator;
    }
    
    /**
     * Update the internal state of the generator.</br>
     * This method is called everytime a new event arrive.</br>
     * By default it reduces by 1 the number of flying packets, but it can be</br>
     * extended to properly update the event generator.</br>
    */
    /*public void update() {
        _packetsInFlight--;
    }*/
    
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
     * This method is called only if the specified departure time is
     * {@link simulator.utils.Time#DYNAMIC DYNAMIC}.</br>
     * In case of fixed departure time just return {@code null}.</br>
     * Returning {@code null} with dynamic time makes the generator to produce no event.
     * 
     * @param e    the input event
     * 
     * @return the departure time.</br>
     * NOTE: returned time can be {@code null}.
    */
    public abstract Time computeDepartureTime( final Event e );
    
    /**
     * Retrieves the session associated with the given event.
     * 
     * @param e    the given event.
    */
    private FlowSession getSession( final Event e )
    {
        FlowSession session;
        if (e == null) {
            session = new FlowSession( FlowSession.nextFlowID() );
            //System.out.println( _agent + ": GENERATA NUOVA SESSIONE: " + session.getId() );
        } else {
            if (_activeGenerator && !_waitResponse) {
                session = new FlowSession( FlowSession.nextFlowID() );
                //System.out.println( _agent + ": GENERATA NUOVA SESSIONE: " + session.getId() );
            } else {
                if ((session = _sessions.get( e.getFlowId() )) == null) {
                    session = new FlowSession( e.getFlowId() );
                    session.setSource( e.getSource() );
                }
            }
        }
        
        //session.setMaximumFlyingPackets( _maxPacketsInFlight );
        session.setMulticast( _isMulticasted );
        session.setMaximumFlyingPackets( _destinations.size() );
        
        _sessions.put( session.getId(), session );
        
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
     * Generates a new list of events.</br>
     * NOTE: time and event can be {@code null}.
     * 
     * @param t    time of the simulator
     * @param e    input event
     * 
     * @return the new event list, or {@code null} if the time is expired.
    */
    public final List<Event> generate( final Time t, final Event e )
    {
        if (!generateEvent( e )) {
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
        
        // Load the current session.
        session = getSession( e );
        
        //if (_agent.getId() == 2)// primo nodo server
        //    System.out.println( "EVENTO RICEVUTO: " + e );
        
        // FIXME controllare che funzioni anche con piu' messaggi contemporanei in input.
        //if (_agent.getId() == 1)
        //    System.out.println( "SONO QUI INPUT: " + e + ", CONTINUE: " + session.continueToSend() );
        
        if (_activeGenerator && session.canSend()) {
            if (_agent.getId() == 1)
                System.out.println( "SONO QUI ACTIVE: " + e );
            
            dummyResEvent.setTime( _time );
            return sendRequest( session, dummyResEvent );
        }
        
        List<Event> events = null;
        if (e instanceof RequestEvent) {
            if (_delayResponse) {
                //if (_agent.getId() == 1)
                //    System.out.println( "SONO QUI DELAYED: " + e );
                
                // Prepare and send the new request packet to the next node.
                dummyResEvent.setTime( _time );
                dummyResEvent.setPacket( e.getPacket() );
                events = sendRequest( session, dummyResEvent );
            } else {
                events = sendResponse( session, e, e.getDestination(), e.getSource() );
            }
        } else {
            session.increaseReceivedPackets();
            // Response message.
            if (e != null && _delayResponse) {
                // Send back the "delayed" response.
                Agent dest = session.getSource();
                events = sendResponse( session, e, _agent, dest );
                if (session.completed()) {
                    _sessions.remove( session.getId() );
                }
            } else {
                if (session.continueToSend()) {
                    //if (_agent.getId() == 1)
                    //    System.out.println( "SONO QUI CONTINUE" );
                    dummyResEvent.setTime( _time );
                    events = sendRequest( session, dummyReqEvent );
                }
            }
        }
        
        return events;
    }
    
    /**
     * Generates a new list of request messages.
     * 
     * @param session    the current session.
     * @param e          the current received event. It can be {@code null}.
     * 
     * @return the list of request messages.
    */
    private List<Event> sendRequest( final FlowSession session, final Event e )
    {
        //if (_agent.getId() == 1)
        //    System.out.println( "CREO IL MESSAGGIO DI RICHIESTA DA: " + e );
        
        List<Event> events = null;
        if (session.canSend()) {
            //_nextDestIndex = selectDestination( e.getArrivalTime() );
            int _nextDestIndex = selectDestination( _time );
            
            //if (_agent.getId() == 1)
            //    System.out.println( "MULTICAST: " + _isMulticasted + ", MULTI_PACKET: " + session.getPacket() );
            
            // Prepare the request packet.
            Packet reqPacket = session.getPacket();
            if (reqPacket == null) {
                reqPacket = makePacket( e, (_isMulticasted) ? -1 : _nextDestIndex );
                session.setPacket( reqPacket );
            }
            
            //if (_agent.getId() == 1)
            //    System.out.println( "INIZIO GENERAZIONE MESSAGGIO DI RICHIESTA DA: " + e );
            
            if (reqPacket != null) {
                session.increaseSentPackets();
                Agent dest = _destinations.get( _nextDestIndex );
                Event request = new RequestEvent( _time, _agent, dest, reqPacket.clone() );
                request.setFlowId( session.getId() );
                events = Collections.singletonList( request );
                
                //if (_agent.getId() == 1)
                //    System.out.println( "GENERATA RICHIESTA: " + request );
            }
        }
        
        return events;
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
        return session.getNextDestination( _destinations.size() );
    }
    
    /**
     * Generates a new list of response messages.
     * 
     * @param session    the current session
     * @param e          the received event.
     * @param from       the source node
     * @param dest       the destination node
     * 
     * @return the list of response messages.
    */
    private List<Event> sendResponse( final FlowSession session, final Event e, final Agent from, final Agent dest )
    {
        Packet resPacket = makePacket( e, session.getSource().getId() );
        
        if (resPacket == null) {
            return null;
        } else {
            Event response = new ResponseEvent( _time, from, dest, resPacket );
            response.setFlowId( session.getId() );
            if (from.getEventHandler() != null) {
                from.getEventHandler().handle( response, EventType.GENERATED );
            }
            response.setArrivalTime( e.getArrivalTime() );
            return Collections.singletonList( response );
        }
    }
}