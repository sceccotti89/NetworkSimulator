/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.concurrent.TimeUnit;

import simulator.Agent;
import simulator.Packet;
import simulator.core.Time;
import simulator.network.NetworkLink;
import simulator.network.NetworkNode;
import simulator.network.NetworkTopology;
import simulator.utils.SimulatorUtils;

public abstract class Event implements Comparable<Event>
{
    /** Event time in microseconds. */
    protected Time _time;
    
    protected Agent _from;
    protected Agent _to;
    
    protected long _currentNodeId = 0;
    
    // Dimension of the "message".
    protected Packet _packet;
    
    
    public Event( final Time time, final Agent from, final Agent to, final Packet packet )
    {
        _time = time;
        _from = from;
        _to = to;
        
        _packet = packet;
        
        _currentNodeId = from.getId();
    }
    
    @Override
    public int compareTo( final Event o ) {
        return _time.compareTo( o.getTime() );
    }
    
    public void execute( final long nodeId, final EventHandler ev_handler, final NetworkTopology net )
    {
        NetworkNode node = net.getNode( nodeId );
        long delay = node.getTcalc();
        
        if (nodeId == _to.getId()) {
            System.out.println( "[" + _time + "] Reached destination node: " + node );
            _time.addTime( new Time( delay, TimeUnit.MICROSECONDS ) );
            ev_handler.schedule( _to.fireEvent( /*_time.clone()*/ ) );
        } else {
            if (nodeId == _from.getId()) {
                System.out.println( "[" + _time + "] Starting from node: " + node );
            } else { 
                System.out.println( "[" + _time + "] Reached intermediate node: " + node );
            }
            NetworkLink link = net.getLink( nodeId, net.nextNode( nodeId, _to.getId() ).getId() );
            //System.out.println( "CURRENT_ID: " + nodeId + ", FOUNDED_LINK: " + link );
            // Assign the current node id.
            _currentNodeId = link.getDestId();
            delay += link.getTtrasm( (long) SimulatorUtils.getSizeInBitFromByte( _packet.getSize(), _packet.getSizeType() ) ) + link.getTprop();
            System.out.println( "AGGIUNTA LATENZA: " + delay );
            _time.addTime( new Time( delay, TimeUnit.MICROSECONDS ) );
            
            // Push back the modified event into the queue.
            ev_handler.schedule( this );
        }
        
        if(!_from.getEventGenerator().waitForResponse())
            ev_handler.schedule( _from.fireEvent() ); // generate a new event, because it doesn't wait for the response.
    }
    
    public Time getTime() {
        return _time;
    }
    
    public long getCurrentNodeId() {
        return _currentNodeId;
    }
    
    
    
    
    /** ======= SPECIALIZED IMPLEMENTATIONS OF EVENT ======= **/
    
    public static class RequestEvent extends Event
    {
        public RequestEvent( final Time time, final Agent from, final Agent to, final Packet pkt )
        {
            super( time, from, to, pkt );
        }
        
        @Override
        public String toString()
        { return "Request: " + _time; }
    }
    
    public static class ResponseEvent extends Event
    {
        public ResponseEvent( final Time time, final Agent from, final Agent to, final Packet packet )
        {
            super( time, from, to, packet );
        }
        
        @Override
        public String toString()
        { return "Response: " + _time; }
    }
}