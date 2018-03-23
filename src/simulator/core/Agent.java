/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simulator.events.Event;
import simulator.events.EventGenerator;
import simulator.events.EventHandler;
import simulator.events.EventHandler.EventType;
import simulator.events.EventScheduler;
import simulator.events.Packet;
import simulator.events.impl.RequestEvent;
import simulator.events.impl.ResponseEvent;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.topology.NetworkNode;
import simulator.utils.Pair;
import simulator.utils.Sampler;
import simulator.utils.Time;

public abstract class Agent extends NetworkAgent
{
    protected long _id;
    
    private EventScheduler _evtScheduler;
    private EventHandler _evtHandler = null;
    private List<EventGenerator> _evtGenerators;
    
    private List<Agent> _destinations;
    
    // Samplings list.
    private Map<String,Sampler> samplings;
    
    
    
    
    public Agent( int channelType, NetworkLayer layer, NetworkNode node )
    {
        this( channelType, layer, node.getId() );
        setNode( node );
    }
    
    public Agent( int channelType, NetworkLayer layer, long id )
    {
        super( channelType, layer );
        
        _id = id;
        
        samplings = new HashMap<>();
        
        _evtGenerators = new ArrayList<>();
        _eventQueue = new ArrayList<>();
        _destinations = new ArrayList<>();
    }
    
    /**
     * Returns the unique agent identifier.
    */
    public long getId() {
        return _id;
    }
    
    public void addEventGenerator( EventGenerator evGenerator )
    {
        _evtGenerators.add( evGenerator );
        evGenerator.setAgent( this );
    }
    
    public void addDevice( Device<?,?> device )
    {
        device.setAgent( this );
        device.setEventScheduler( _evtScheduler );
        //_devices.put( device.getID(), device );
        _devices.put( device.getClass(), device );
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Device<?,?>> T getDevice( T device ) {
        return (T) getDevice( device.getClass() );
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Device<?,?>> T getDevice( Class<T> device ) {
        return (T) _devices.get( device );
        /*try {
            Method method = device.getMethod( "getID" );
            return getDevice( (String) method.invoke( device.newInstance() ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }*/
    }
    
    /*public <T extends Device<?,?>> T getDevice( T device ) {
        return getDevice( device.getID() );
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Device<?,?>> T getDevice( String id ) {
        return (T) _devices.get( id );
    }*/
    
    public Collection<Device<?,?>> getDevices() {
        return _devices.values();
    }
    
    public void connect( Agent destination )
    {
        _destinations.add( destination );
        destination._destinations.add( this );
    }
    
    public List<Agent> getConnectedAgents() {
        return _destinations;
    }
    
    public Agent getConnectedAgent( long id )
    {
        return _destinations.stream()
                            .filter( agent -> agent.getId() == id )
                            .findAny()
                            .orElse( null );
    }
    
    /**
     * Sends a message to the specified destination node.
     * 
     * @param destination    the destination node.
     * @param message        the message to send.
     * @param request        {@code true} for a request message (see {@link RequestEvent}),
     *                       {@code false} for a reponse message (see see {@link ResponseEvent}).
    */
    public void sendMessage( Agent destination, Packet message, boolean request ) {
        sendMessage( getTime(), destination, message, request );
    }
    
    /**
     * Sends a message to the specified destination node, at a specific time.
     * 
     * @param time           time when the message is sent.
     * @param destination    the destination node.
     * @param message        the message to send.
     * @param request        {@code true} for a request message (see {@link RequestEvent}),
     *                       {@code false} for a reponse message (see see {@link ResponseEvent}).
    */
    public void sendMessage( Time time, Agent destination, Packet message, boolean request )
    {
        Event e = (request) ? new RequestEvent( time, this, destination, message ) : 
                              new ResponseEvent( time, this, destination, message );
        _evtScheduler.schedule( e );
        if (_evtHandler != null) {
            _evtHandler.handle( e, EventType.GENERATED );
        }
    }
    
    /**
     * Adds a new sampler to collect some informations about this device.
     * 
     * @param samplerId    identifier of the sampler. Must be UNIQUE.
     * @param sampler      the sampler object.
     * 
     * @throws RuntimeException if the samplerId already exists.
    */
    public void addSampler( String samplerId, Sampler sampler )
    {
        if (samplings.containsKey( samplerId )) {
            throw new RuntimeException( "Selected name \"" + samplerId + "\" already exists." );
        }
        samplings.put( samplerId, sampler );
    }
    
    /**
     * Returns the requested sampler.
     * 
     * @param sampler    the sampler identifier.
    */
    public Sampler getSampler( String sampler ) {
        return samplings.get( sampler );
    }

    public void setEventScheduler( EventScheduler evtScheduler )
    {
        _evtScheduler = evtScheduler;
        for (Device<?,?> device : _devices.values()) {
            device.setEventScheduler( evtScheduler );
        }
    }
    
    public EventScheduler getEventScheduler() {
        return _evtScheduler;
    }
    
    /**
     * Returns the requested event generator.
     * 
     * @param index    index of the requested event generator.
    */
    public EventGenerator getEventGenerator( int index ) {
        return _evtGenerators.get( index );
    }
    
    public EventGenerator getEventGenerator( EventGenerator generator ) {
        return _evtGenerators.get( _evtGenerators.indexOf( generator ) );
    }
    
    /**
     * Called when an event is received by this agent.
     * 
     * @param e    the input event
    */
    public void receivedMessage( Event e ) {
        // TODO Renderlo astratto?
        _eventQueue.add( e );
    }
    
    /**
     * Removes an event from the queue.
     * 
     * @param event    the event to remove
     * 
     * @return e    the removed event
    */
    public Event removeEventFromQueue( Event event )
    {
        _eventQueue.remove( event );
        return event;
    }
    
    /**
     * Removes an event from the queue.
     * 
     * @param index    position of the event to remove
     * 
     * @return the removed event
    */
    public Event removeEventFromQueue( int index )
    {
        if (!_eventQueue.isEmpty()) {
            return _eventQueue.remove( index );
        }
        return null;
    }
    
    /**
     * Checks if the current event can be executed.</br>
     * In presence of any device an override of this method is suggested.</br>
     * NOTE: this method sets also the time of the event, in case the given one can't be executed.
     * 
     * @param eventTime    time of the event
     * 
     * @return {@code true} if the event can be executed immediately,
     *         {@code false} otherwise
    */
    public boolean canExecute( Time eventTime )
    {
        boolean execute = eventTime.compareTo( _time ) >= 0;
        if (!execute) {
            // Set the time of the agent.
            eventTime.setTime( _time );
        }
        return execute;
    }
    
    public void addEventHandler( EventHandler evtHandler ) {
        _evtHandler = evtHandler;
    }
    
    public EventHandler getEventHandler() {
        return _evtHandler;
    }
    
    public List<Event> getQueue() {
        return _eventQueue;
    }

    /**
     * Returns the percentage of node utilization.</br>
     * By default it returns the size of the associated event queue.</br>
     * In case of any attached device an override of this method is suggested.
     * 
     * @param time    time when the queue is checked.
    */
    public double getNodeUtilization( Time time ) {
        return _eventQueue.size();
    }
    
    /**
     * Returns the next list of events.
     * 
     * @param t    current simulation time
     * @param e    current event
    */
    public final List<Event> fireEvent(/* Time t, Event e */)
    {
        // Gets the events generated by the network protocols (if any).
        List<Event> events = new ArrayList<>();
        /*for (NetworkProtocol protocol : _node.getNetworkSettings().getRoutingProtocols()) {
            // TODO protocol.processPacket( e.getPacket() );
            events.add( protocol.getEvent() );
        }*/
        
        Time duration = _evtScheduler.getTimeDuration();
        for (EventGenerator evGenerator : _evtGenerators) {
            /*if (e != null && e.getSource().getId() == getId() &&
                e.getGeneratorID() != evGenerator.getId()) {
                continue;
            }*/
            
            //Event event = evGenerator.generate( t, e );
            Event event = evGenerator.generate();
            if (event != null && event.getTime().compareTo( duration ) <= 0) {
                events.add( event );
            }
        }
        
        // Removes all the events whose time is higher than the simulation time.
        /*for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get( i );
            if (event.getTime().compareTo( duration ) > 0) {
                events.remove( i );
            }
        }*/
        
        return (events.isEmpty() ? null : events);
    }

    /**
     * Closes all the resources opened by this agent.
    */
    public void shutdown() throws IOException
    {
        for (Device<?,?> device : _devices.values()) {
            try {
                device.shutdown();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        
        for (Sampler sampler : samplings.values()) {
            String logFile = sampler.getLogFile();
            if (logFile != null) {
                PrintWriter writer = new PrintWriter( logFile, "UTF-8" );
                for (Pair<Double,Double> point : sampler.getValues()) {
                    writer.println( point.getFirst() + " " + point.getSecond() );
                }
                writer.close();
            }
        }
    }
    
    @Override
    public String toString() {
        return "Id: " + _id;
    }
}
