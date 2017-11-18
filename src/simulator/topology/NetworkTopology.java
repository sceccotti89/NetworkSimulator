/**
 * @author Stefano Ceccotti
*/

package simulator.topology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import simulator.utils.resources.ResourceLoader;

public class NetworkTopology
{
	private Map<Long,NetworkNode> nodes = new HashMap<>( 32 );
	private Map<Long,List<NetworkLink>> links = new HashMap<>( 32 );
	private Map<Long,Agent> agents;
	@SuppressWarnings("deprecation")
    private GraphPath gp = new GraphPath();
	
	private long netID;
	
	private PrintWriter eventsWriter;
	
	private EventScheduler evtScheduler;
	
	private int _nextIndex = 0;
	private static long nextID = 0;
    



    public NetworkTopology( JSONObject settings ) throws IOException
    {
        netID = getNextID();
        build( settings );
        agents = new HashMap<>();
    }
	
    public NetworkTopology( String filename ) throws IOException
    {
        netID = getNextID();
        agents = new HashMap<>();
        try { build( filename ); }
        catch( IOException e ) {
            System.err.println( "File '" + filename + "' not found." );
            throw e;
        }
    }
    
    /**
     * Builds the network topology.
     * 
     * @param filename  name of the file where nodes and links are loaded from
    */
    private void build( String filename ) throws IOException
    {
        InputStream stream = ResourceLoader.getResourceAsStream( filename );
        BufferedReader br = new BufferedReader( new InputStreamReader( stream ) );
        StringBuilder content = new StringBuilder( 512 );
        
        String nextLine = null;
        while ((nextLine = br.readLine()) != null) {
            content.append( nextLine.trim() );
        }
        
        JSONObject settings = new JSONObject( content.toString() );
        build( settings );
        
        br.close();
    }
    
    /**
     * Builds the network topology.
     * 
     * @param filename  name of the file where nodes and links are loaded from
    */
    private void build( JSONObject settings ) throws IOException
    {
        /** File structure:
         * 
         * nodes => [{id, name, delay, [xPos, yPos]}]
         * links => [{fromId, destId, bw, delay, [linkType]}]
        */
        
        // Get the list of nodes.
        JSONArray nodes = settings.getJSONArray( "nodes" );
        int length = nodes.length();
        for (int i = 0; i < length; i++) {
            JSONObject node = nodes.getJSONObject( i );
            long id         = node.getLong( NetworkNode.ID );
            String name     = node.getString( NetworkNode.NAME );
            long delay      = node.getLong( NetworkNode.DELAY );
            int xPos        = (node.has( NetworkNode.X_POS )) ? node.getInt( NetworkNode.X_POS ) : 0;
            int yPos        = (node.has( NetworkNode.Y_POS )) ? node.getInt( NetworkNode.Y_POS ) : 0;
            
            NetworkNode _node = new NetworkNode( this, id, name, delay, xPos, yPos );
            addNode( _node );
        }
        
        // Get the list of links.
        JSONArray links = settings.getJSONArray( "links" );
        length = links.length();
        for (int i = 0; i < length; i++) {
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
    
    public void setEventScheduler( EventScheduler evtScheduler )
    {
        this.evtScheduler = evtScheduler;
        evtScheduler.setNetwork( this );
        for (Agent agent : agents.values()) {
            agent.setEventScheduler( evtScheduler );
        }
    }
    
    public void addAgent( Agent agent )
    {
        if (!containsNode( agent.getId() ))
            throw new SimulatorException( "No node found for ID: " + agent.getId() );
        
        agent.setNode( getNode( agent.getId() ) );
        agent.setEventScheduler( evtScheduler );
        agents.put( agent.getId(), agent );
    }
    
    public void addAgents( Agent... agents )
    {
        for (Agent agent : agents) {
            addAgent( agent );
        }
    }
    
    public Collection<Agent> getAgents() {
        return agents.values();
    }

    public void addLink( long fromId, long destId,
                         double bandwith, long delay,
                         String linkType )
    {
        addLink( new NetworkLink( fromId, destId, bandwith, delay, linkType ) );
    }
    
    public void addLink( NetworkLink link )
    {
        if (!nodes.containsKey( link.getSourceId() ))
        	throw new SimulatorException( "Node '" + link.getSourceId() + "' not found." );
        if (!nodes.containsKey( link.getDestId() ))
        	throw new SimulatorException( "Node '" + link.getDestId() + "' not found." );
        
    	insertLink( link );
    }
    
    private void insertLink( NetworkLink link )
    {
        List<NetworkLink> sLinks = links.get( link.getSourceId() );
        if (sLinks == null) sLinks = new ArrayList<NetworkLink>();
        sLinks.add( link );
        links.put( link.getSourceId(), sLinks );
    }
    
    public void connectToNetwork( NetworkTopology net,
                                  long fromId, long destId,
                                  double bandwith, long delay,
                                  String linkType )
    {
        connectToNetwork( net, new NetworkLink( fromId, destId, bandwith, delay, linkType ) );
    }
    
    public void connectToNetwork( NetworkTopology net, NetworkLink link )
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
    
    public NetworkLink getLink( long sourceId, long destId )
    {
    	for (NetworkLink link : links.get( sourceId )) {
    		if (link.getDestId() == destId)
    			return link;
    	}
    	return null;
    }
    
    public void addNode( long id, String name, long delay,
                         int xPos, int yPos ) {
        addNode( new NetworkNode( this, id, name, delay, xPos, yPos ) );
    }
    
    public void addNode( NetworkNode node )
    {
    	node.setIndex( _nextIndex++ );
        nodes.put( node.getId(), node );
    }
    
    public boolean containsNode( long nodeId ) {
        return nodes.containsKey( nodeId );
    }
    
    public NetworkNode getNode( long nodeId ) {
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
     * topology during the simulation (i.e. add/remove links or nodes), at some fixed time
     * specified in the event.
     * 
     * @param events    the "external" events
    */
    public void addExternalEvents( List<ExternalEvent> events ) {
        for (ExternalEvent event : events) {
            evtScheduler.schedule( event );
        }
    }
    
    /**
     * This method permits to add an "external" event to modify the current network
     * topology during the simulation (i.e. add/remove links or nodes), at some fixed time
     * specified in the event.
     * 
     * @param event    the "external" event
    */
    public void addExternalEvent( ExternalEvent event ) {
        evtScheduler.schedule( event );
    }
    
    /**
     * Asks the network to track the incoming event, as a string message.</br>
     * The given message will be saved on file only if a previous call to the method
     * {@linkplain #setTrackingEvent(String)} is done.
     * 
     * @param message    the input message.
    */
    public void trackEvent( String message ) {
        if (eventsWriter != null) {
            eventsWriter.println( message );
        }
    }
    
    /**
     * Sets the file used to keep tracks of the generated events.
     * 
     * @param eventsFile    name of the file.
    */
    public void setTrackingEvent( String eventsFile ) throws FileNotFoundException
    {
        File file = new File( eventsFile );
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        eventsWriter = new PrintWriter( eventsFile );
    }

    // TODO questi 2 metodi non serviranno se implemento i protocolli di routing.
    /**
     * Computes the shortest path for every node in the network.
    */
    @SuppressWarnings("deprecation")
    public void computeShortestPaths() {
        gp.computeAllSourcesShortestPath( this, nodes, links );
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
    public NetworkNode nextNode( long sourceId, long destId )
    {
    	@SuppressWarnings("deprecation")
        NetworkNode[] predecessors = gp.getShortestPath( sourceId, this, nodes, links );
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
        if (eventsWriter != null) {
            eventsWriter.close();
        }
    }
    
    private static final synchronized long getNextID() {
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
