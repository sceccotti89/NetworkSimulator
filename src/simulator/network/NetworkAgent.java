/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Device;
import simulator.events.Event;
import simulator.events.impl.AgentEvent;
import simulator.network.protocols.EventProtocol;
import simulator.network.protocols.EventProtocol.TimeoutEvent;
import simulator.network.protocols.Header;
import simulator.network.protocols.NetworkProtocol;
import simulator.network.protocols.Protocol;
import simulator.network.protocols.ProtocolReference;
import simulator.network.protocols.TransportProtocol;
import simulator.network.protocols.impl.Ethernet;
import simulator.network.protocols.impl.IP.IPv4;
import simulator.network.protocols.impl.IP.IPv6;
import simulator.network.protocols.impl.TCP;
import simulator.network.protocols.impl.UDP;
import simulator.topology.NetworkNode;
import simulator.topology.NetworkTopology;
import simulator.utils.Time;

// TODO inserire i tempi di avvio e durata dell'agent??
public abstract class NetworkAgent
{
    protected Time _time;
    
    protected NetworkNode _node;
    protected NetworkTopology _net;
    
    protected List<Event> _eventQueue;
    
    protected Map<Class<?>,Device<?,?>> _devices;
    
    protected NetworkSettings _settings;
    private List<Integer> _availablePorts;
    private final Random randPortGen = new Random( 64511 );
    private Map<String,Connection> connections;
    //private Map<String,Connection> user_connections;
    private NetworkLayer layer;
    
    // Type of channel: Simplex, Half-duplex or Full-duplex.
    protected int channelType;
    
    private boolean _parallelTransmission = false;
    
    protected boolean closed = false;
    
    // By default the buffer has a size of 4K bytes.
    private int bufferSize = 1 << 12;
    
    public static final int SIMPLEX = 0;
    public static final int HALF_DUPLEX = 1;
    public static final int FULL_DUPLEX = 2;
    
    
    
    public NetworkAgent( int channelType, NetworkLayer layer )
    {
        _time = new Time( 0, TimeUnit.MICROSECONDS );
        
        _eventQueue = new ArrayList<>();
        
        this.layer = layer;
        connections = new HashMap<>();
        //user_connections = new HashMap<>();
        _availablePorts = new ArrayList<>( 64511 );
        for (int i = 0; i < 64511; i++) {
            _availablePorts.add( i+1024 );
        }
        
        _devices = new HashMap<>();
        
        this.channelType = channelType;
        
        _settings = new NetworkSettings();
        _settings.addNetworkProtocol( new UDP() );
        _settings.addNetworkProtocol( new TCP() );
        _settings.addNetworkProtocol( new IPv4() );
        _settings.addNetworkProtocol( new IPv6() );
        // TODO Per adesso lo rimuovo da qui..
        //_settings.addNetworkProtocol( new Ethernet() );
        
        // TODO Lo scrivo qui: un nodo dovrebbe eseguire la cosiddetta "autonegoziazione",
        // TODO ovvero dovrebbe scambiare (a livello fisico) le informazioni sulla priopria configurazione (half o full duplex).
        // TODO questa cosa la faccio in maniera istantanea, altrimenti non ci esco piu'.
    }
    
    public int getChannelType() {
        return channelType;
    }
    
    public NetworkSettings getNetSettings() {
        return _settings;
    }
    
    /**
     * Sets the time of the agent.</br>
     * The internal time of the agent will be updated only if the input time is greater
     * than the current one.
     * 
     * @param now    the current time
    */
    public void setTime( Time now )
    {
        if (now.compareTo( _time ) > 0) {
            _time.setTime( now );
            for (Device<?,?> device : _devices.values()) {
                device.setTime( now );
            }
        }
    }

    public Time getTime() {
        return _time.clone();
    }
    
    public void setNetworkTopology( NetworkTopology net ) {
        _net = net;
    }
    
    public void setNode( NetworkNode node )
    {
        _node = node;
        node.setAgent( (Agent) this );
    }
    
    public NetworkNode getNode() {
        return _node;
    }
    
    protected Connection waitForConnection( TransportProtocol protocol )
    {
        Connection conn = new Connection( (Agent) this, _net, protocol );
        connections.put( protocol.getIdentifier(), conn );
        //user_connections.put(  );
        conn.setBufferSpace( bufferSize );
        return conn;
    }
    
    protected Connection createConnection( TransportProtocol protocol )
    {
        Connection conn = new Connection( (Agent) this, _net, protocol );
        connections.put( protocol.getIdentifier(), conn );
        conn.setBufferSpace( bufferSize );
        return conn;
    }
    
    protected Connection createConnection( String destAddress, TransportProtocol protocol )
    {
        Connection conn = new Connection( (Agent) this, destAddress, _net, protocol );
        connections.put( protocol.getIdentifier(), conn );
        conn.setBufferSpace( bufferSize );
        
        if (!protocol.connectionDone()) {
            // Start the hand-shake phase.
            conn.sendMessage( destAddress, null );
        }
        
        return conn;
    }
    
    protected Connection getConnection( Object... params )
    {
        if (params == null || params.length == 0) {
            // TODO Ritornare errore
            
        }
        
        StringBuilder builder = new StringBuilder( 32 );
        final int length = params.length;
        for (int i = 0; i < length - 1; i++) {
            builder.append( params[i] + Protocol.SEPARATOR );
        }
        builder.append( params[length-1] );
        return connections.get( builder.toString() );
    }
    
    private Protocol getProtocol( int layer, Integer type )
    {
        if (type == null) {
            return null;
        } else {
            List<Protocol> protocols = _settings.getNetworkProtocols( layer );
            if (protocols != null) {
                for (Protocol protocol : protocols) {
                    if (protocol instanceof TransportProtocol) {
                        if (((TransportProtocol) protocol).getProtocol() == type) {
                            return protocol;
                        }
                    }
                    if (protocol instanceof NetworkProtocol) {
                        if (((NetworkProtocol) protocol).getEtherType() == type) {
                            return protocol;
                        }
                    }
                    // TODO Completare con gli altri protocoli?? in teoria dovrei..
                }
            }
            return null;
        }
    }
    
    public void setBufferSize( int size ) {
        bufferSize = size;
    }
    
    public int getBufferSize() {
        return bufferSize;
    }
    
    public void receivedMessage( Event event )
    {
        if (event instanceof TimeoutEvent) {
            TimeoutEvent timeout = (TimeoutEvent) event;
            int layer = timeout.getLayer();
            int type  = timeout.getIdentifier();
            EventProtocol protocol = (EventProtocol) getProtocol( layer, type );
            protocol.processEvent( timeout );
            return;
        }
        
        //_net.trackEvent( "" );
        
        // TODO capire come utilizzarlo: se non mi serve lo elimino!!!!!
        //_eventQueue.add( event );
        
        Header message = (Header) event;
        ProtocolReference ref = null;
        // TODO Rimuovere ethernet da qui o dal costruttore, oppure recuperare ethernet in un altro modo
        Protocol protocol = new Ethernet();
        while (protocol != null &&
               protocol.getLayer().getIndex() < layer.getIndex() &&
               message.getSizeInBits() > 0)
        {
            ref = protocol.processHeader( message );
            if (ref == null) {
                break;
            }
            // Get the next protocol.
            protocol = getProtocol( protocol.getLayer().getIndex() + 1, ref.getNextProtocol() );
        }
        
        // The message is empty.
        if (message.getSizeInBits() == 0) {
            return;
        }
        
        Connection conn = connections.get( ref.getNextProtocol() + "" );
        if (conn != null) {
            if (conn.checkBufferSpace( message.getSizeInBytes() )) {
                Header response = ref.getResponse();
                if (response != null) {
                    conn.send( protocol.getLayer().getIndex(), response );
                }
                //conn.addReceivedMessage( message );
                //receivedMessage( new Message( message.getBytes() ), conn );
                
                AgentEvent e = new AgentEvent( getTime(), (Agent) this );
                e.setMessage( message, conn );
                ((Agent) this).getEventScheduler().schedule( e );
            }
        }
    }
    
    /**
     * Executes an incoming event.
     * 
     * @param event    the event to execute.
    */
    public void executeEvent( Event event )
    {
        AgentEvent e = (AgentEvent) event;
        Header message = e.getMessage();
        Connection conn = e.getConnection();
        conn.freeBufferSpace( message.getSizeInBytes() );
        //receivedMessage( new Message( message.getBytes() ), conn );
    }
    
    /**
     * Setting true this flag, makes the agent don't pay the transmission delay.
     * 
     * @param flag    
    */
    public void setParallelTransmission( boolean flag ) {
        _parallelTransmission = flag;
    }
    
    public boolean isParallelTransmission() {
        return _parallelTransmission;
    }
    
    /**
     * Notify an incoming event. This method is called anytime a self-event is generated.
     * 
     * @param e    the generated event.
    */
    public abstract void notifyEvent( Event e );
    
    /**
     * Removes the given port from the available ones.
     * 
     * @param port    the port to remove.
    */
    public void removeAvailablePort( int port )
    {
        for (int index = 0; index < _availablePorts.size(); index++) {
            if (_availablePorts.get( index ) == port) {
                _availablePorts.remove( index );
                break;
            }
        }
    }
    
    /**
     * Sets a port as available.
     * 
     * @param port    the available port.
    */
    protected void setAvailablePort( int port )
    {
        int index;
        for (index = 0; index < _availablePorts.size(); index++) {
            if (_availablePorts.get( index ) > port) {
                break;
            }
        }
        _availablePorts.add( index, port );
    }
    
    /**
     * Returns a random available port.
     * 
     * @return an available port, or {@code null} if there's none.
    */
    public Integer getAvailablePort()
    {
        if (_availablePorts.isEmpty()) {
            return null;
        } else {
            // Random port generator.
            return _availablePorts.remove( randPortGen.nextInt( 64511 ) );
        }
    }
    
    public boolean isClosed() {
        return closed;
    }
}
