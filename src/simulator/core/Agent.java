/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.events.Event;
import simulator.events.EventGenerator;
import simulator.events.EventHandler;
import simulator.events.EventScheduler;
import simulator.network.protocols.NetworkProtocol;
import simulator.topology.NetworkNode;
import simulator.utils.Time;

public abstract class Agent
{
    protected long _id;
    protected NetworkNode _node;
    
    private EventScheduler _evtScheduler;
    private EventHandler _evtHandler = null;
    protected EventGenerator _evGenerator;
    protected List<Agent> _destinations;
    
    private Time _time;
    
    private Map<String,Device<?,?>> _devices;
    
    private List<Event> _eventQueue;
    
    private boolean _parallelTransmission = false;
    
    
    
    
    public Agent( final NetworkNode node ) {
        this( node.getId() );
    }
    
    public Agent( final long id ) {
        this( id, null );
    }
    
    public Agent( final NetworkNode node, final EventGenerator evGenerator ) {
        this( node.getId(), evGenerator );
    }
    
    public Agent( final long id, final EventGenerator evGenerator )
    {
        _id = id;
        _evGenerator = evGenerator;
        _destinations = new ArrayList<>();
        _devices = new HashMap<>();
        
        _eventQueue = new ArrayList<>();
        
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        if (_evGenerator != null)
            _evGenerator.setAgent( this );
    }
    
    public void connect( final Agent destination )
    {
        _destinations.add( destination );
        _evGenerator.connect( destination );
    }
    
    public void connectAll( final List<Agent> destinations )
    {
        _destinations.addAll( destinations );
        _evGenerator.connectAll( destinations );
    }
    
    public long getId() {
        return _id;
    }
    
    public NetworkNode getNode() {
        return _node;
    }
    
    public void addDevice( final Device<?,?> device ) {
        device.setEventScheduler( _evtScheduler );
        _devices.put( device.getID(), device );
    }
    
    public <T extends Device<?,?>> T getDevice( final T device ) {
        return getDevice( device.getID() );
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Device<?,?>> T getDevice( final String id ) {
        return (T) _devices.get( id );
    }
    
    public Collection<Device<?,?>> getDevices() {
        return _devices.values();
    }
    
    public Time getTime() {
        return _time.clone();
    }
    
    public void setNode( final NetworkNode node )
    {
        _node = node;
        node.setAgent( this );
    }
    
    public void setEventScheduler( final EventScheduler evtScheduler ) {
        _evtScheduler = evtScheduler;
        for (Device<?,?> device : _devices.values()) {
            device.setEventScheduler( evtScheduler );
        }
    }
    
    public EventScheduler getEventScheduler() {
        return _evtScheduler;
    }
    
    /**
     * Put an input event into the queue.
     * 
     * @param e    the input event
    */
    public void addEventOnQueue( final Event e ) {
        _eventQueue.add( e );
    }
    
    /**
     * Remove an event from the queue.
     * 
     * @param event    the event to remove
     * 
     * @return e    the removed event
    */
    public Event removeEventFromQueue( final Event event ) {
        _eventQueue.remove( event );
        return event;
    }
    
    /**
     * Remove an event from the queue.
     * 
     * @param index    position of the event to remove
     * 
     * @return e    the removed event
    */
    public Event removeEventFromQueue( final int index ) {
        if (!_eventQueue.isEmpty()) {
            return _eventQueue.remove( index );
        }
        return null;
    }
    
    /**
     * Cheks if the current event can be executed.</br>
     * In presence of any device an override of this method is suggested.</br>
     * NOTE: this method sets also the time of the event, in case the given one can't be executed.
     * 
     * @param eventTime    time of the event
     * 
     * @return {@code true} if the event can be executed immediately,
     *         {@code false} otherwise
    */
    public boolean canExecute( final Time eventTime)
    {
        boolean execute = eventTime.compareTo( _time ) >= 0;
        if (!execute) {
            // Set the time of the agent.
            eventTime.setTime( _time );
        }
        return execute;
    }
    
    public void addEventHandler( final EventHandler evtHandler ) {
        _evtHandler = evtHandler;
    }
    
    public EventHandler getEventHandler() {
        return _evtHandler;
    }
    
    public List<Event> getQueue() {
        return _eventQueue;
    }
    
    /**
     * Setting true this flag, makes the agent don't pay the transmission delay.
     * 
     * @param flag    
    */
    public Agent setParallelTransmission( final boolean flag ) {
        _parallelTransmission = flag;
        return this;
    }
    
    public boolean parallelTransmission() {
        return _parallelTransmission;
    }
    
    /**
     * Returns the next list of events.
     * 
     * @param t    current simulation time
     * @param e    current event
    */
    public List<Event> fireEvent( final Time t, final Event e )
    {
        // Gets the events generated by the protocols (if any).
        List<Event> events = new ArrayList<>();
        for (NetworkProtocol protocol : _node.getNetworkSettings().getRoutingProtocols()) {
            events.add( protocol.getEvent() );
        }
        
        if (_evGenerator != null) {
            List<Event> genEvents = _evGenerator.generate( t, e );
            if (genEvents != null) {
                events.addAll( genEvents );
            }
        }
        
        return (events.isEmpty() ? null : events);
    }
    
    /**
     * Sets the time of the agent.</br>
     * The internal time of the agent will be updated only if the input time is greater
     * than the current one.
     * 
     * @param now    set the current time
    */
    public void setTime( final Time now ) {
        if (_time.compareTo( now ) < 0) {
            _time = now;
        }
    }
    
    /**
     * Returns the percentage of node utilization.</br>
     * By default it returns the size of the event queue.</br>
     * In case of any attached device an override of this method is suggested.
     * 
     * @param time    time when the queue is checked.
    */
    public double getNodeUtilization( final Time time ) {
        return _eventQueue.size();
    }
    
    /**
     * Method used to close all the resources opened by this agent.
    */
    public void shutdown() {
        for (Device<?,?> device : _devices.values()) {
            try {
                device.shutdown();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public String toString() {
        return "Id: " + _id;
    }
}