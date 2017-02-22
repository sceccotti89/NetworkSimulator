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
    
    
    public Event( final Time time, final Agent from, final Agent to )
    {
        _time = time;
        _from = from;
        _to = to;
    }
    
    @Override
    public int compareTo( final Event o ) {
        return _time.compareTo( o.getTime() );
    }
    
    public abstract void execute( final EventHandler ev_handler );
    
    public Time getTime() {
        return _time;
    }
    
    public static class RequestEvent extends Event
    {
        public RequestEvent( final Time time, final Agent from, final Agent to )
        {
            super( time, from, to );
        }

        @Override
        public void execute( final EventHandler ev_handler ) {
            // TODO Auto-generated method stub
            
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
        public void execute( final EventHandler ev_handler ) {
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public String toString()
        { return "Response: " + _time; }
    }
}