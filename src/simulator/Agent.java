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
    
    public void connect( final List<Agent> destinations )
    {
        _destinations.addAll( destinations );
        _evGenerator.connect( destinations );
    }
    
    //TODO public abstract void body();
    
    public long getId() {
        return _id;
    }
    
    public EventGenerator getEventGenerator() {
        return _evGenerator;
    }

    /**
     * Put the first message into the queue.</br>
     * In case of {@link PassiveAgent} no action will be performed,</br>
     * if {@link ActiveAgent} the first event is taken form it.
    */
    public Event firstEvent()
    {
        if ( _evGenerator != null && _evGenerator.isActive()) {
            return _evGenerator.generate( null, null );
        } else {
            return null;
        }
    }
    
    /***/
    public Event fireEvent( final Time t, final Event e )
    {
        if (_evGenerator != null) {
            return _evGenerator.generate( t, e );
        } else {
            return null;
        }
    }
    
    
    
    
    
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