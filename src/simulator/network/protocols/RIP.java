/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.events.impl.ProtocolEvent;
import simulator.network.NetworkLayer;
import simulator.network.protocols.NetworkProtocol.ApplicationLayerProtocol;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

public class RIP extends ApplicationLayerProtocol
{
    private static final Time UPDATE_TIME = new Time( 30, TimeUnit.SECONDS );
    
    public RIP( final NetworkTopology net, final Agent agent )
    {
        super( net, agent );
        setLayer( NetworkLayer.APPLICATION.getIndex() );
        protocols.put( NetworkLayer.TRANSPORT, new UDP( agent.getAvailablePort(), 520 ) );
    }
    
    @Override
    public NetworkNode getNextNode( final long destID ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public ProtocolEvent getEvent()
    {
        // TODO generare un pacchetto contenente il contenuto della propria tabella di routing.
        //makePacket();
        ProtocolEvent event = new ProtocolEvent( time.clone(), agent, getID() );
        time.addTime( UPDATE_TIME );
        return event;
    }
    
    @Override
    public Packet makePacket() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Event processPacket( final Packet packet ) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public String getID() {
        return "RIP";
    }
}