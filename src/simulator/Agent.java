/**
 * @author Stefano Ceccotti
*/

package simulator;

import java.util.ArrayList;
import java.util.List;

import simulator.coordinator.Event;
import simulator.coordinator.EventGenerator;
import simulator.core.Time;
import simulator.network.NetworkNode;

public abstract class Agent
{
    protected long _id;
    protected EventGenerator _evGenerator;
    protected List<Agent> _destinations;
    
    protected long _lastArrival = 0;
    protected long _elapsedTime = 0;
    
    public Agent( final NetworkNode node )
    { this( node.getId(), null ); }
    
    public Agent( final long id )
    { this( id, null ); }
    
    public Agent( final NetworkNode node, final EventGenerator evGenerator )
    { this( node.getId(), evGenerator ); }
    
    public Agent( final long id, final EventGenerator evGenerator )
    {
        _id = id;
        _evGenerator = evGenerator;
        _destinations = new ArrayList<>();
        
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
    
    /**
     * Returns the next list of events.
     * 
     * @param t    
     * @param e    
    */
    public List<Event> fireEvent( final Time t, final Event e )
    {
        if (_evGenerator != null) {
            return _evGenerator.generate( t, e );
        } else {
            return null;
        }
    }
    
    /**
     * Sets the time elapsed among two arrived packets.
     * 
     * @param time    time when the packet arrive
    */
    public void setElapsedTime( final long time )
    {
        _elapsedTime = time - _lastArrival;
        _lastArrival = time;
    }
    
    /**
     * Analyze the incoming packet.
     * 
     * @param p    the incoming packet
    */
    public abstract void analyzePacket( final Packet p );
    
    
    
    
    
    // TODO queste classi sono inutili, per adesso.
    /*public static abstract class ActiveAgent extends Agent
    {
        public ActiveAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
    }
    
    public static abstract class PassiveAgent extends Agent
    {
        public PassiveAgent( final long id )
        {
            super( id );
        }
        
        public PassiveAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
    }
    
    public static abstract class ActiveAndPassiveAgent extends Agent
    {
        public ActiveAndPassiveAgent( final long id )
        {
            super( id );
        }
        
        public ActiveAndPassiveAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
    }*/
}