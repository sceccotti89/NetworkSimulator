/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import simulator.core.Time;

public abstract class Event implements Comparable<Event>
{
    /** Event time in microseconds. */
    protected Time time;
    
    
    public Event( final Time time ) {
        this.time = time;
    }
    
    @Override
    public int compareTo( final Event o ) {
        return time.compareTo( o.getTime() );
    }
    
    public abstract void execute( EventHandler ev_handler );
    
    public Time getTime() {
        return time;
    }
    
    public static class RequestEvent extends Event
    {
        public RequestEvent( final Time time )
        {
            super( time );
        }

        @Override
        public void execute( final EventHandler ev_handler ) {
            // TODO Auto-generated method stub
            
        }
    }
    
    public static class ResponseEvent extends Event
    {
        public ResponseEvent( final Time time )
        {
            super( time );
        }

        @Override
        public void execute( final EventHandler ev_handler ) {
            // TODO Auto-generated method stub
            
        }
    }
}