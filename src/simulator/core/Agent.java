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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import simulator.events.Event;
import simulator.events.EventHandler;
import simulator.events.EventScheduler;
import simulator.events.generator.EventGenerator;
import simulator.network.protocols.NetworkProtocol;
import simulator.topology.NetworkNode;
import simulator.utils.Time;

public abstract class Agent
{
    protected long _id;
    protected NetworkNode _node;
    
    private EventScheduler _evtScheduler;
    private EventHandler _evtHandler = null;
    protected List<EventGenerator> _evtGenerators;
    private List<Event> _eventQueue;
    
    private boolean _parallelTransmission = false;
    
    private Time _time;
    
    private Map<String,Device<?,?>> _devices;
    
    private List<Integer> _availablePorts;
    
    
    
    
    public Agent( final NetworkNode node ) {
        this( node.getId() );
        setNode( node );
    }
    
    public Agent( final long id )
    {
        _id = id;
        _devices = new HashMap<>();
        
        _evtGenerators = new ArrayList<>();
        _eventQueue = new ArrayList<>();
        
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _availablePorts = new ArrayList<>( 64511 );
        for (int i = 0; i < 64511; i++) {
            _availablePorts.add( i+1024 );
        }
    }
    
    public long getId() {
        return _id;
    }
    
    public NetworkNode getNode() {
        return _node;
    }
    
    public void addEventGenerator( final EventGenerator evGenerator )
    {
        _evtGenerators.add( evGenerator );
        evGenerator.setAgent( this );
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
    
    /**
     * Sets the time of the agent.</br>
     * The internal time of the agent will be updated only if the input time is greater
     * than the current one.
     * 
     * @param now    set the current time
    */
    public void setTime( final Time now )
    {
        if (now.compareTo( _time ) > 0) {
            _time.setTime( now );
            for (Device<?,?> device : getDevices()) {
                device.setTime( now );
            }
        }
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
     * Returns the requested event generator.
     * 
     * @param index    index of the requested event generator.
    */
    public EventGenerator getEventGenerator( final int index ) {
        return _evtGenerators.get( index );
    }

    /**
     * Puts an input event into the queue.
     * 
     * @param e    the input event
    */
    public void addEventOnQueue( final Event e ) {
        _eventQueue.add( e );
    }
    
    /**
     * Removes an event from the queue.
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
     * Removes an event from the queue.
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
     * Checks if the current event can be executed.</br>
     * In presence of any device an override of this method is suggested.</br>
     * NOTE: this method sets also the time of the event, in case the given one can't be executed.
     * 
     * @param eventTime    time of the event
     * 
     * @return {@code true} if the event can be executed immediately,
     *         {@code false} otherwise
    */
    public boolean canExecute( final Time eventTime )
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
    
    public boolean isParallelTransmission() {
        return _parallelTransmission;
    }
    
    /**
     * Returns the percentage of node utilization.</br>
     * By default it returns the size of the associated event queue.</br>
     * In case of any attached device an override of this method is suggested.
     * 
     * @param time    time when the queue is checked.
    */
    public double getNodeUtilization( final Time time ) {
        return _eventQueue.size();
    }
    
    /**
     * Sets a port as available.
     * 
     * @param port    the available port.
    */
    public void setAvailablePort( final int port )
    {
        int index;
        for (index = 0; index < _availablePorts.size(); index++) {
            if (_availablePorts.get( index ) > port) {
                break;
            }
        }
        _availablePorts.add( index, port );
    }
    
    /**
     * Returns a random available port.
     * 
     * @return an available port, or {@code null} if there's none.
    */
    public Integer getAvailablePort()
    {
        if (_availablePorts.isEmpty()) {
            return null;
        } else {
            // Random port generator.
            Random rand = new Random( 64511 );
            return _availablePorts.remove( rand.nextInt( 64511 ) );
        }
    }
    
    /**
     * Returns the next list of events.
     * 
     * @param t    current simulation time
     * @param e    current event
    */
    public List<Event> fireEvent( final Time t, final Event e )
    {
        // Gets the events generated by the network protocols (if any).
        List<Event> events = new ArrayList<>();
        for (NetworkProtocol protocol : _node.getNetworkSettings().getRoutingProtocols()) {
            // TODO protocol.processPacket( e.getPacket() );
            events.add( protocol.getEvent() );
        }
        
        for (EventGenerator evGenerator : _evtGenerators) {
            if (e != null && e.getSource().getId() == getId() &&
                e.getGeneratorID() != evGenerator.getId()) {
                continue;
            }
            
            Event event = evGenerator.generate( t, e );
            if (event != null) {
                events.add( event );
            }
        }
        
        // Removes all the events whose time is higher than the simulation time.
        Time duration = _evtScheduler.getTimeDuration();
        for (int i = events.size() - 1; i >= 0; i--) {
            Event event = events.get( i );
            if (event.getTime().compareTo( duration ) > 0) {
                events.remove( i );
            }
        }
        
        return (events.isEmpty() ? null : events);
    }

    /**
     * Closes all the resources opened by this agent.
    */
    public void shutdown()
    {
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