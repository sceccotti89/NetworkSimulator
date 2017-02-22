/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import simulator.Agent;
import simulator.core.Time;

public abstract class Event implements Comparable<Event>
{
    /** Event time in microseconds. */
    protected Time _time;
    
    protected Agent _from;
    protected Agent _to;
    
    protected long _currentNodeId = 0;
    
    // Dimension of the "message".
    protected long _size;
    
    
    public Event( final Time time, final Agent from, final Agent to )
    {
        _time = time;
        _from = from;
        _to = to;
        
        _currentNodeId = from.getId();
    }
    
    @Override
    public int compareTo( final Event o ) {
        return _time.compareTo( o.getTime() );
    }
    
    public abstract void execute( final long nodeId, final EventHandler ev_handler );
    
    public Time getTime() {
        return _time;
    }
    
    public long getCurrentNodeId() {
        return _currentNodeId;
    }
    
    
    
    
    /** ======= SPECIALIZED IMPLEMENTATIONS OF THE EVENT ======= **/
    
    public static class RequestEvent extends Event
    {
        public RequestEvent( final Time time, final Agent from, final Agent to )
        {
            super( time, from, to );
        }

        @Override
        public void execute( final long nodeId, final EventHandler ev_handler )
        {
            if(nodeId == _to.getId())
                System.out.println( "RAGGIUNTA DESTINAZIONE!!" );// TODO raggiunta la destinazione controlla se generare la risposta
            else
                ;// TODO modifica l'evento corrente e lo rimette in coda
            
            if(!_from.getEventGenerator().waitForResponse())
                ev_handler.schedule( _from.fireEvent() ); // generate a new event, because it doesn't wait for the response.
        }
        
        @Override
        public String toString()
        { return "Request: " + _time; }
    }
    
    public static class ResponseEvent extends Event
    {
        public ResponseEvent( final Time time, final Agent from, final Agent to )
        {
            super( time, from, to );
        }

        @Override
        public void execute( final long nodeId, final EventHandler ev_handler ) {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public String toString()
        { return "Response: " + _time; }
    }
}