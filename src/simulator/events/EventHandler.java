/**
 * @author Stefano Ceccotti
*/

package simulator.events;

import simulator.topology.NetworkNode;
import simulator.utils.Time;

/**
 * Handler for events.</br>
 * Any action on it is valid, just don't add or remove events from the associated agent queue,
 * since it's done by the event itself.
*/
public interface EventHandler
{
    public static enum EventType{
        GENERATED,
        RECEIVED,
        SENT
    };
    
    /**
     * Invoked when a specific event for which this handler is registered happens.</br>
     * The simplest implementation is returning the calculation time associated
     * with the {@link NetworkNode} (see {@link NetworkNode#getTcalc() getTcalc()} method).
     * 
     * @param e       the event which occurred
     * @param type    type of handled event
     * 
     * @return time needed to handle the event.
    */
    public Time handle( Event e, EventType type );
}
