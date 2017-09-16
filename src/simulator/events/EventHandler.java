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
     *  The simplest implementation is to return the time associated
     * to the {@link NetworkNode} ({@link NetworkNode#getTcalc() getTcalc()} method),
     * ensuring that it's NOT {@link Time#DYNAMIC dynamic}.
     * 
     * @param e       the event which occurred
     * @param type    type of handled event
     * 
     * @return time needed to handle the event.
    */
    public Time handle( final Event e, final EventType type );
}