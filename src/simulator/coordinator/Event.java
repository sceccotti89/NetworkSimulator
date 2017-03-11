/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import java.util.Collections;
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
    
    // Message payload.
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
    
    public Time getTime() {
        return _time;
    }
    
    public Agent getSource() {
        return _from;
    }
    
    public Agent getDest() {
        return _to;
    }
    
    public long getCurrentNodeId() {
        return _currentNodeId;
    }
    
    public Packet getPacket() {
        return _packet;
    }
    
    public void execute( final long nodeId, final EventScheduler ev_handler, final NetworkTopology net )
    {
        NetworkNode node = net.getNode( nodeId );
        
        if (nodeId == _to.getId()) {
            System.out.println( "[" + _time + "] Reached destination node: " + node );
            _to.setElapsedTime( _time.getTimeMicroseconds() );
            _to.analyzePacket( _packet );
            _time.addTime( node.getTcalc(), TimeUnit.MICROSECONDS );
            // Prepare and schedule the response event.
            ev_handler.schedule( _to.fireEvent( _time, this ) );
        } else {
            long delay = 0;
            if (nodeId == _from.getId()) {
                System.out.println( "[" + _time + "] Starting from node: " + node );
            } else {
                delay = node.getTcalc();
                System.out.println( "[" + _time + "] Reached intermediate node: " + node );
            }
            
            NetworkLink link = net.getLink( nodeId, net.nextNode( nodeId, _to.getId() ).getId() );
            if (link != null) {
                // Assign the current node id.
                _currentNodeId = link.getDestId();
                long Ttrasm = link.getTtrasm( (long) SimulatorUtils.getSizeInBitFromByte( _packet.getSize(), _packet.getSizeType() ) );
                
                ev_handler.schedule( _from.fireEvent( _time.clone().addTime( Ttrasm, TimeUnit.MICROSECONDS ), null ) );
                
                delay += Ttrasm + link.getTprop();
                
                System.out.println( "Ttrasm: " + ((double) Ttrasm)/((double) SimulatorUtils.MILLION) + "s" );
                System.out.println( "Tprop:  " + ((double) link.getTprop())/((double) SimulatorUtils.MILLION) + "s" );
                _time.addTime( delay, TimeUnit.MICROSECONDS );
                
                // Push-back the modified event into the queue.
                ev_handler.schedule( Collections.singletonList( this ) );
            }
        }
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
        { return "Request Event: " + _time + "ns"; }
    }
    
    public static class ResponseEvent extends Event
    {
        public ResponseEvent( final Time time, final Agent from, final Agent to, final Packet packet )
        {
            super( time, from, to, packet );
        }
        
        @Override
        public String toString()
        { return "Response Event: " + _time + "ns"; }
    }
}