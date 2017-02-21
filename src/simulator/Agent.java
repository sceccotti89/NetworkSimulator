/**
 * @author Stefano Ceccotti
*/

package simulator;

import simulator.coordinator.Event;
import simulator.coordinator.EventGenerator;
import simulator.coordinator.EventHandler;

public abstract class Agent
{
    protected long _id;
    protected EventGenerator _evGenerator;
    
    public Agent( final long id )
    {
        _id = id;
    }
    
    public Agent( final long id, final EventGenerator evGenerator )
    {
        _id = id;
        _evGenerator = evGenerator;
    }
    
    /***/
    public void addGenerator( final EventGenerator evGenerator )
    {
        _evGenerator = evGenerator;
    }
    
    //TODO public abstract void body();
    
    public long getId() {
        return _id;
    }
    
    /**
     * Put the first message into the queue.</br>
     * In case of {@link PassiveAgent} no action will be performed,</br>
     * if {@link ActiveAgent} the first event is taken form it.
     * 
     * @param evHandler handler used to manage all the events
    */
    public abstract Event firstEvent( final EventHandler evHandler );
    
    public static abstract class ActiveAgent extends Agent
    {
        public ActiveAgent( final long id )
        {
            super( id );
        }
        
        public ActiveAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
        }
        
        @Override
        public Event firstEvent( final EventHandler evHandler )
        { return _evGenerator.nextEvent( evHandler ); }
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
        
        @Override
        public Event firstEvent( final EventHandler evHandler )
        { return null; }
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
        
        @Override
        public Event firstEvent( final EventHandler evHandler )
        { return _evGenerator.nextEvent( evHandler ); }
    }
}