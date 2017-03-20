/**
 * @author Stefano Ceccotti
*/

package simulator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.core.Device;
import simulator.core.Time;
import simulator.manager.Event;
import simulator.manager.EventGenerator;
import simulator.network.NetworkNode;

public abstract class Agent
{
    protected long _id;
    protected NetworkNode _node;
    
    protected EventGenerator _evGenerator;
    protected List<Agent> _destinations;
    
    private Time _time;
    
    protected long _lastArrival = 0;
    protected long _elapsedTime = 0;
    
    private Map<String,Device> _devices;
    
    private List<Event> eventQueue;
    
    
    
    
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
        
        eventQueue = new ArrayList<>();
        
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
    
    public void addDevice( final Device device ) {
        _devices.put( device.getID(), device );
        device.setAgent( this );
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Device> T getDevice( final T device ) {
        return (T) _devices.get( device.getID() );
    }
    
    public Collection<Device> getDevices() {
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
    
    /**
     * Checks for a duplicated event on queue.
    */
    public boolean checkEventOnQueue( final long eventId )
    {
        for (Event e : eventQueue) {
            if (e.getId() == eventId) {
                return true;
            }
        }
        return false;
    }
    
    public void addEventOnQueue( final Event e ) {
        eventQueue.add( e );
    }
    
    public void removeEventOnQueue( final int index ) {
        if (!eventQueue.isEmpty()) {
            eventQueue.remove( index );
        }
    }
    
    public List<Event> getQueue() {
        return eventQueue;
    }
    
    /**
     * Returns the next list of events.
     * 
     * @param t    current simulation time
     * @param e    current event
    */
    public List<Event> fireEvent( final Time t, final Event e )
    {
        if (t != null) {
            updateTime( t.clone() );
        }
        
        if (_evGenerator != null) {
            return _evGenerator.generate( t, e );
        } else {
            return null;
        }
    }
    
    /**
     * Sets the time elapsed of two consecutive arrived packets.
     * 
     * @param time    time of the last arrived packet
    */
    public void setElapsedTime( final long time )
    {
        _elapsedTime = time - _lastArrival;
        _lastArrival = time;
        
        if (_time.getTimeMicroseconds() == 0) {
            _time.addTime( _lastArrival, TimeUnit.MICROSECONDS );
        }
    }
    
    public void updateTime( final Time now ) {
        _time = now;
    }
    
    /**
     * Analyze the incoming event.</br>
     * This method is user-defined, but if the event doesn't need to be analyzed</br>
     * just leave it empty (return {@code null}).
     * 
     * @param time    time when the packet arrived
     * @param e       the incoming event
    */
    public abstract Time analyzeEvent( final Time time, final Event e );
}