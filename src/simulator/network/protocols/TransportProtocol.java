/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.core.Agent;
import simulator.events.EventScheduler;
import simulator.network.NetworkLayer;

public abstract class TransportProtocol extends Protocol
{
    protected EventScheduler scheduler;
    
    public TransportProtocol( NetworkLayer layer, Protocol... baseProtocols ) {
        super( layer );
    }
    
    @Override
    public void setAgent( Agent node )
    {
        setScheduler( node.getEventScheduler() );
        super.setAgent( node );
    }
    
    private void setScheduler( EventScheduler scheduler) {
        this.scheduler = scheduler;
    }
    
    /**
     * Checks whether the connection has been performed.
     * 
     * @return {@code true} if the connection has been completed,
     *         {@code false} otherwise.
    */
    public abstract boolean connectionDone();
    
    /**
     * Returns the associated Protocol Identifier.
    */
    public abstract int getProtocol();
    
    /**
     * Returns the associated identifier.
    */
    public abstract String getIdentifier();
}
