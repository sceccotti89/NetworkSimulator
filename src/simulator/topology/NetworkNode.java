/**
 * @author Stefano Ceccotti
*/

package simulator.topology;

import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.network.NetworkSettings;
import simulator.utils.Time;

/**
 * Class used to model the simplest network units, such as
 * clients, servers, routers, switches, hubs, and even mobile nodes.
*/
public class NetworkNode
{
    private final long   _id;
    private final String _name;
    
    private boolean _active = true;
    private final Time _delay;
    
    private final int _xPos;
    private final int _yPos;
    
    private NetworkSettings _settings;
    
    // Note: used internally for the shortest path calculation.
    private int _index;
    
    // Associated agent.
    private Agent _agent;
    
    public static final String ID = "id", NAME = "name", DELAY = "delay";
    public static final String X_POS = "xPos", Y_POS = "yPos";
    
    
    
    
    
    public NetworkNode( long id, String name, long delay ) {
        this( id, name, delay, 0, 0 );
    }
    
    public NetworkNode( long id, String name, long delay, int xPos, int yPos )
    {
        _id = id;
        _name = name;
        
        _delay = new Time( delay, TimeUnit.MILLISECONDS );
        
        _xPos = xPos;
        _yPos = yPos;
        _settings = new NetworkSettings();
    }
    
    public long getId() {
        return _id;
    }
    
    public String getName() {
        return _name;
    }
    
    /**
     * Calculation time of the node.
    */
    public Time getTcalc() {
        return _delay.clone();
    }
    
    public int getXPos() {
        return _xPos;
    }
    
    public int getYPos() {
        return _yPos;
    }
    
    public void setAgent( Agent agent ) {
        _agent = agent;
    }
    
    public Agent getAgent() {
        return _agent;
    }
    
    public void setIndex( int index ) {
    	_index = index;
    }
    
    public int getIndex() {
    	return _index;
    }
    
    public NetworkSettings getNetworkSettings() {
        return _settings;
    }
    
    public Event analyzePacket( Packet packet )
    {
        /*final String protocol_ID = packet.getContent( "Protocol_ID" );
        for (NetworkProtocol protocol : _settings.getRoutingProtocols()) {
            if (protocol.getID().equals( protocol_ID )) {
                return protocol.processPacket( packet );
            }
        }*/
        return null;
    }

    public void setActive( boolean flag ) {
        _active = flag;
    }
    
    public boolean isActive() {
        return _active;
    }
    
    @Override
    public String toString()
    {
        String settings = _settings.toString();
        if (settings.isEmpty()) {
            return "{Id: " + _id + ", Name: \"" + _name +
                    "\", Delay: " + ((_delay.isDynamic()) ? "Dynamic}\n" : _delay + "ns}\n");
        } else {
            return "{Id: " + _id + ", Name: \"" + _name +
                    "\", Delay: " + ((_delay.isDynamic()) ? "Dynamic\n" : _delay + "ns\n") +
                    settings + "}\n";
        }
    }
}
