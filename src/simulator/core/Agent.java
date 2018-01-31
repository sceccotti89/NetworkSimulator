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
import simulator.events.EventHandler;
import simulator.events.EventScheduler;
import simulator.events.generator.EventGenerator;
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
    
    private boolean _parallelTransmission = false;
    
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
     * Puts an input event into the queue.
     * 
     * @param e    the input event
    */
    public void addEventOnQueue( Event e ) {
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
     * Setting true this flag, makes the agent don't pay the transmission delay.
     * 
     * @param flag    
    */
    public Agent setParallelTransmission( boolean flag ) {
        _parallelTransmission = flag;
        return this;
    }
    
    public boolean isParallelTransmission() {
        return _parallelTransmission;
    }
    
    
    /**
     * Adds a new value to the specified sampler with the corresponding time intervals.</br>
     * If the starting time is earlier than the ending time,
     * the given value is "distributed" in multiple buckets along the entire interval;
     * if the sampler interval is less or equal than 0 it goes in a single separate bucket,
     * whose insertion is driven by the ending time.</br>
     * 
     * @param sampler        the specified sampler in which insert the value.
     * @param startTime      starting time of the event.
     * @param endTime        ending time of the event.
     * @param value          value to add.
    */
    public void addSampledValue( String sampler, Time startTime, Time endTime, double value ) {
        addSampledValue( sampler, startTime.getTimeMicros(), endTime.getTimeMicros(), value );
    }

    /**
     * Adds a new value to the specified sampler with the corresponding time intervals.</br>
     * If the starting time is earlier than the ending time,
     * the given value is "distributed" in multiple buckets along the entire interval;
     * if the sampler interval is less or equal than 0 it goes in a single separate bucket,
     * whose insertion is driven by the ending time.</br>
     * NOTE: all times MUST be expressed in microseconds.
     * 
     * @param sampler        the specified sampler in which insert the value.
     * @param startTime      starting time of the event.
     * @param endTime        ending time of the event.
     * @param value          value to add.
    */
    public void addSampledValue( String sampler, double startTime, double endTime, double value )
    {
        Sampler samplerObj = samplings.get( sampler );
        if (samplerObj != null) {
            samplerObj.addSampledValue( startTime, endTime, value );
        }
    }
    
    /**
     * Returns the list of values sampled by the requested sampler.
     * 
     * @param sampler    the requested sampler
     * 
     * @return {@code null} if the requested sampler is not present,
     *         its list of values otherwise.
    */
    public List<Pair<Double,Double>> getSampledValues( String sampler )
    {
        if (!samplings.containsKey( sampler )) {
            return null;
        } else {
            return samplings.get( sampler ).getValues();
        }
    }
    
    /**
     * Returns the sum of all the results sampled by the given sampler.
     * 
     * @param sampler    the requested sampler
     * 
     * @return {@code null} if the requested sampler is not present,
     *         its result value otherwise.
    */
    public Double getResultSampled( String sampler )
    {
        if (!samplings.containsKey( sampler )) {
            return null;
        } else {
            return samplings.get( sampler ).getTotalResult();
        }
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
    public final List<Event> fireEvent( Time t, Event e )
    {
        // Gets the events generated by the network protocols (if any).
        List<Event> events = new ArrayList<>();
        /*for (NetworkProtocol protocol : _node.getNetworkSettings().getRoutingProtocols()) {
            // TODO protocol.processPacket( e.getPacket() );
            events.add( protocol.getEvent() );
        }*/
        
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
    public void shutdown() throws IOException
    {
        for (Device<?,?> device : _devices.values()) {
            try {
                device.shutdown();
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
        
        if (samplings != null) {
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
    }
    
    @Override
    public String toString() {
        return "Id: " + _id;
    }
}
