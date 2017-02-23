/**
 * @author Stefano Ceccotti
*/

package simulator;

import simulator.coordinator.Event;
import simulator.coordinator.EventGenerator;
import simulator.network.NetworkNode;

public abstract class Agent
{
    protected long _id;
    protected EventGenerator _evGenerator;
    protected Agent _dest;
    
    public Agent( final NetworkNode node )
    { this( node.getId(), null ); }
    
    public Agent( final long id )
    { this( id, null ); }
    
    public Agent( final NetworkNode node, final EventGenerator evGenerator )
    {
        _id = node.getId();
        _evGenerator = evGenerator;
        
    }
    
    public Agent( final long id, final EventGenerator evGenerator )
    {
        _id = id;
        _evGenerator = evGenerator;
        
    }
    
    public void connect( final Agent destination )
    {
        _dest = destination;
        _evGenerator.connect( this, destination );
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
     * 
     * @param evHandler handler used to manage all the events
    */
    public abstract Event firstEvent();
    
    /***/
    public Event fireEvent()
    {
        if(_evGenerator != null)
            return _evGenerator.generate();
        else
            return null;
    }
    
    
    
    
    
    /***/
    public static abstract class ActiveAgent extends Agent
    {
        public ActiveAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
        
        @Override
        public Event firstEvent()
        { return _evGenerator.generate(); }
    }
    
    /***/
    public static abstract class PassiveAgent extends Agent
    {
        public PassiveAgent( final long id )
        {
            super( id );
        }
        
        @Override
        public Event firstEvent()
        { return null; }
    }
    
    /***/
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
        
        @Override
        public Event firstEvent()
        { return _evGenerator.generate(); }
    }
}