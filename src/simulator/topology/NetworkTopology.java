/**
 * @author Stefano Ceccotti
*/

package simulator.topology;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import simulator.core.Agent;
import simulator.events.EventScheduler;
import simulator.events.impl.ExternalEvent;
import simulator.exception.SimulatorException;

public class NetworkTopology
{
	private Map<Long,NetworkNode> nodes = new HashMap<>( 32 );
	private Map<Long,List<NetworkLink>> links = new HashMap<>( 32 );
	private Map<Long,Agent> agents;
	
	private static long nextID = 0;
	private long netID;
	
	private PrintWriter eventsWriter;
	
	private EventScheduler evtScheduler;
	
	private int _nextIndex = 0;
	
	
    
    public NetworkTopology( final JSONObject settings ) throws IOException
    {
        netID = getNextID();
        build( settings );
        agents = new HashMap<>();
    }
	
	public NetworkTopology( final String filename ) throws IOException
    {
	    netID = getNextID();
        try { build( filename ); }
        catch( IOException e ) {
            System.err.println( "File '" + filename + "' not found." );
            throw e;
        }
        
        agents = new HashMap<>();
    }
    
    /**
     * Builds the network topology
     * 
     * @param filename  name of the file where nodes and links are loaded from
    */
    private void build( final String filename ) throws IOException
    {
        BufferedReader br = new BufferedReader( new FileReader( filename ) );
        StringBuilder content = new StringBuilder( 512 );
        
        String nextLine = null;
        while((nextLine = br.readLine()) != null)
            content.append( nextLine.trim() );
        
        JSONObject settings = new JSONObject( content.toString() );
        build( settings );
        
        br.close();
    }
    
    /**
     * Builds the network topology
     * 
     * @param filename  name of the file where nodes and links are loaded from
    */
    private void build( final JSONObject settings ) throws IOException
    {
        /** File structure:
         * 
         * nodes => [{id, name, delay, [xPos, yPos]}]
         * links => [{fromId, destId, bw, delay, [linkType]}]
        */
        
        // Get the list of nodes.
        JSONArray nodes = settings.getJSONArray( "nodes" );
        int length = nodes.length();
        for(int i = 0; i < length; i++) {
            JSONObject node = nodes.getJSONObject( i );
            long id         = node.getLong( NetworkNode.ID );
            String name     = node.getString( NetworkNode.NAME );
            long delay      = node.getLong( NetworkNode.DELAY );
            int xPos        = (node.has( NetworkNode.X_POS )) ? node.getInt( NetworkNode.X_POS ) : 0;
            int yPos        = (node.has( NetworkNode.Y_POS )) ? node.getInt( NetworkNode.Y_POS ) : 0;
            
            NetworkNode _node = new NetworkNode( this, id, name, delay, xPos, yPos );
            addNode( _node );
            _node.toString();
        }
        
        // Get the list of links.
        JSONArray links = settings.getJSONArray( "links" );
        length = links.length();
        for(int i = 0; i < length; i++) {
            JSONObject link = links.getJSONObject( i );
            long fromId     = link.getLong( NetworkLink.FROM_ID );
            long destId     = link.getLong( NetworkLink.DEST_ID );
            double bandwith = link.getDouble( NetworkLink.BANDWITH );
            long delay      = link.getLong( NetworkLink.DELAY );
            String linkType = (link.has( NetworkLink.LINK_TYPE )) ?
                                    link.getString( NetworkLink.LINK_TYPE ) :
                                    NetworkLink.UNIDIRECTIONAL;
            
            NetworkLink _link = new NetworkLink( fromId, destId, bandwith, delay, linkType );
            addLink( _link );
            
            if(linkType.equals( NetworkLink.BIDIRECTIONAL )) {
                addLink( _link.reverse() );
            }
        }
    }
    
    public long getId() {
        return netID;
    }
    
    public void setEventScheduler( final EventScheduler evtScheduler ) {
        this.evtScheduler = evtScheduler;
        evtScheduler.setNetwork( this );
        for (Agent agent : agents.values()) {
            agent.setEventScheduler( evtScheduler );
        }
    }
    
    public void addAgent( final Agent agent )
    {
        if (!containsNode( agent.getId() ))
            throw new SimulatorException( "No node found for ID: " + agent.getId() );
        
        agent.setNode( getNode( agent.getId() ) );
        agent.setEventScheduler( evtScheduler );
        agents.put( agent.getId(), agent );
    }
    
    public void addAgents( final List<Agent> agents )
    {
        for (Agent agent : agents) {
            addAgent( agent );
        }
    }
    
    public Collection<Agent> getAgents() {
        return agents.values();
    }

    public void addLink( final long fromId, final long destId,
                         final double bandwith, final long delay,
                         final String linkType )
    {
        addLink( new NetworkLink( fromId, destId, bandwith, delay, linkType ) );
    }
    
    public void addLink( final NetworkLink link )
    {
        if (!nodes.containsKey( link.getSourceId() ))
        	throw new SimulatorException( "Node '" + link.getSourceId() + "' not found." );
        if (!nodes.containsKey( link.getDestId() ))
        	throw new SimulatorException( "Node '" + link.getDestId() + "' not found." );
        
    	insertLink( link );
    }
    
    private void insertLink( final NetworkLink link )
    {
        List<NetworkLink> sLinks = links.get( link.getSourceId() );
        if(sLinks == null) sLinks = new ArrayList<NetworkLink>();
        sLinks.add( link );
        links.put( link.getSourceId(), sLinks );
    }
    
    public void connectToNetwork( final NetworkTopology net,
                                  final long fromId, final long destId,
                                  final double bandwith, final long delay,
                                  final String linkType )
    {
        connectToNetwork( net, new NetworkLink( fromId, destId, bandwith, delay, linkType ) );
    }
    
    public void connectToNetwork( final NetworkTopology net, final NetworkLink link )
    {
        if (!nodes.containsKey( link.getSourceId() ))
            throw new SimulatorException( "Node '" + link.getSourceId() + "' not found." );
        if (!net.containsNode( link.getDestId() ))
            throw new SimulatorException( "Node '" + link.getDestId() + "' not found." );
        
        insertLink( link );
        if (link.linkType() == NetworkLink.BIDIRECTIONAL) {
            net.insertLink( link.reverse() );
        }
    }
    
    public NetworkLink getLink( final long sourceId, final long destId )
    {
    	for (NetworkLink link : links.get( sourceId )) {
    		if (link.getDestId() == destId)
    			return link;
    	}
    	return null;
    }
    
    public void addNode( final long id, final String name, final long delay,
                         final int xPos, final int yPos ) {
        addNode( new NetworkNode( this, id, name, delay, xPos, yPos ) );
    }
    
    public void addNode( final NetworkNode node )
    {
    	node.setIndex( _nextIndex++ );
        nodes.put( node.getId(), node );
    }
    
    public boolean containsNode( final long nodeId ) {
        return nodes.containsKey( nodeId );
    }
    
    public NetworkNode getNode( final long nodeId ) {
        return nodes.get( nodeId );
    }
    
    public Collection<NetworkNode> getNodes() {
        return nodes.values();
    }
    
    public EventScheduler getEventScheduler() {
        return evtScheduler;
    }
    
    /**
     * This method permits to add some "external" events to modify the current network
     * topology during the simulation, adding or removing links or nodes, at some fixed time
     * specified in the event.
     * 
     * @param events    the "external" events
    */
    public void addExternalEvents( final List<ExternalEvent> events ) {
        for (ExternalEvent event : events) {
            evtScheduler.schedule( event );
        }
    }
    
    /**
     * This method permits to add some "external" event to modify the current network
     * topology during the simulation, adding or removing links or nodes, at some fixed time
     * specified in the event.
     * 
     * @param event    the "external" event
    */
    public void addExternalEvent( final ExternalEvent event ) {
        evtScheduler.schedule( event );
    }
    
    /**
     * Asks the network to track the incoming event, as a string message.</br>
     * The given message will be saved on file only if a previous call to the method
     * {@linkplain #setTrackingEvent(String)} is done.
     * 
     * @param message    the input message.
    */
    public void trackEvent( final String message ) {
        if (eventsWriter != null) {
            eventsWriter.println( message );
        }
    }
    
    /**
     * Sets the file use to keep tracks of the generated events.
     * 
     * @param eventsFile    name of the file.
    */
    public void setTrackingEvent( final String eventsFile ) throws FileNotFoundException {
        eventsWriter = new PrintWriter( eventsFile );
    }

    // TODO questi 2 metodi non serviranno se implemento i protocolli di routing.
    /**
     * Computes the shortest path for every node in the network.
    */
    @SuppressWarnings("deprecation")
    public void computeShortestPaths() {
        GraphPath.computeAllSourcesShortestPath( this, nodes, links );
    }
    
    /**
     * Returns the next node, according to some internal rules (e.g. shortest path),</br>
     * starting from the source and arriving to the destination.
     * 
     * @param sourceId    starting node identifier
     * @param destId      destination node identifier
     * 
     * @return the next node, if founded, {@code null} otherwise.
    */
    public NetworkNode nextNode( final long sourceId, final long destId )
    {
    	@SuppressWarnings("deprecation")
        NetworkNode[] predecessors = GraphPath.getShortestPath( sourceId, this, nodes, links );
    	NetworkNode currNode = nodes.get( destId ), nextNode = null;
    	while (predecessors[currNode.getIndex()] != null) {
    		nextNode = predecessors[currNode.getIndex()];
    		if (nextNode.getId() == sourceId) {
    			break;
    		} else {
    			currNode = nextNode;
    		}
    	}
    	return currNode;
    }
    
    public void shutdown()
    {
        for (Agent agent : agents.values()) {
            agent.shutdown();
        }
        evtScheduler.shutdown();
        eventsWriter.close();
    }
    
    private static final long getNextID() {
        return nextID = (nextID + 1) % Long.MAX_VALUE;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 512 );
        
        buffer.append( "Nodes information:\n" );
        for (NetworkNode node : nodes.values())
            buffer.append( node.toString() );
        
        buffer.append( "\nLinks information:\n" );
        for (List<NetworkLink> _links : links.values()) {
            for(NetworkLink link : _links)
            	buffer.append( link.toString() );
        }
        
        return buffer.toString();
    }
}