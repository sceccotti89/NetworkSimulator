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
    
    public int compareTo( final Event o ) {
        return time.compareTo( o.getTime() );
    }
    
    public abstract void execute( EventHandler ev_handler );
    
    public Time getTime() {
        return time;
    }
}