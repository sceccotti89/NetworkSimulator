/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.core.Time;
import simulator.network.NetworkLink;
import simulator.network.NetworkNode;
import simulator.network.NetworkTopology;

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
    
    public abstract void execute( final long nodeId, final EventHandler ev_handler, final NetworkTopology net );
    
    public Time getTime() {
        return _time;
    }
    
    public long getCurrentNodeId() {
        return _currentNodeId;
    }
    
    
    
    
    /** ======= SPECIALIZED IMPLEMENTATIONS OF AN EVENT ======= **/
    
    public static class RequestEvent extends Event
    {
        public RequestEvent( final Time time, final Agent from, final Agent to )
        {
            super( time, from, to );
        }
        
        @Override
        public void execute( final long nodeId, final EventHandler ev_handler, final NetworkTopology net )
        {
            // TODO in questa fase dovrebbe aggiungere il tempo speso per trasmettere il pacchetto,
            // TODO o lo puo' fare prima di questa fase?
            if(nodeId == _to.getId())
                ev_handler.schedule( _to.fireEvent() );
            else { // Assign the current node id.
                _currentNodeId = nodeId;
                // TODO aggiungi all'evento la latenza del nodo (Tcal processing del pacchetto) +
                // TODO tempo di trasferimento (Ttrasm = PktSize / Bwt) +
                // TODO latenza del collegamento (Tprop)
                NetworkNode node = net.getNode( nodeId );
                long delay = node.getTcalc();
                NetworkLink link = net.getLink( nodeId, net.nextNode( nodeId ) );
                delay += link.getTtrasm( _size ) + link.getTprop();
                _time.addTime( new Time( delay, TimeUnit.MICROSECONDS ) );
            }
            
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
        public void execute( final long nodeId, final EventHandler ev_handler, final NetworkTopology net )
        {
            // TODO questo metodo e' il solito dl RequestEvent?? in tal caso metterlo in Event.
            
        }
        
        @Override
        public String toString()
        { return "Response: " + _time; }
    }
}