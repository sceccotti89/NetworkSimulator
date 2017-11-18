/**
 * @author Stefano Ceccotti
*/

package simulator.events.impl;

import simulator.events.Event;
import simulator.events.EventScheduler;
import simulator.topology.NetworkLink;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public class ExternalEvent extends Event
{
    public enum EventMode{ RECOVER, SHUTDOWN };
    
    private long fromID = -1;
    private long destID = -1;
    private EventMode mode;
    
    
    
    /** Constructor used for nodes. */
    public ExternalEvent( Time time, long id, EventMode mode ) {
        this( time, id, -1, mode );
    }
    
    /** Constructor used for links. */
    public ExternalEvent( Time time, long fromID, long destID, EventMode mode ) {
        super( time );
        this.fromID = fromID;
        this.destID = destID;
        this.mode = mode;
    }
    
    @Override
    public void execute( EventScheduler ev_scheduler, NetworkTopology net )
    {
        if (destID != -1) { // Link.
            NetworkLink link = net.getLink( fromID, destID );
            link.setActive( mode == EventMode.RECOVER );
        } else { // Node.
            NetworkNode node = net.getNode( fromID );
            node.setActive( mode == EventMode.RECOVER );
        }
    }
}
