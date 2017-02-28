/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import simulator.exception.SimulatorException;

public class NetworkTopology
{
	private Map<Long,NetworkNode> nodes = new HashMap<>( 32 );
	private Map<Long,List<NetworkLink>> links = new HashMap<>( 32 );
	
	private int _nextIndex = 0;

	public NetworkTopology()
    { }
    
    public NetworkTopology( final String filename ) throws IOException, SimulatorException
    {
        try { build( filename ); }
        catch( IOException e ) {
            System.err.println( "File '" + filename + "' not found." );
            throw e;
        }
    }
    
    /**
     * Builds the network topology
     * 
     * @param filename  name of the file where nodes and links are loaded from
     * @throws SimulatorException 
    */
    private void build( final String filename ) throws IOException, SimulatorException
    {
        BufferedReader br = new BufferedReader( new FileReader( filename ) );
        StringBuilder content = new StringBuilder( 512 );
        
        /** File structure:
         * 
         * nodes => [{[xPos],[yPos],id, name, delay}]
         * links => [{fromId, destId, bw, delay, [linkType]}]
        */
        
        String nextLine = null;
        while((nextLine = br.readLine()) != null)
            content.append( nextLine.trim() );
        
        JSONObject settings = new JSONObject( content.toString() );
        
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
            
            NetworkNode _node = new NetworkNode( id, name, delay, xPos, yPos );
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
            int linkType    = (link.has( NetworkLink.LINK_TYPE )) ? link.getInt( NetworkLink.LINK_TYPE ) : 0;
            
            NetworkLink _link = new NetworkLink( fromId, destId, bandwith, delay, linkType );
            addLink( _link );
            
            if(linkType == NetworkLink.BIDIRECTIONAL) {
                _link = new NetworkLink( destId, fromId, bandwith, delay, linkType );
                addLink( _link );
            }
        }
        
        br.close();
    }
    
    public void addLink( final long fromId, final long destId,
                         final double bandwith, final long delay,
                         final int linkType ) throws SimulatorException
    {
        addLink( new NetworkLink( fromId, destId, bandwith, delay, linkType ) );
    }
    
    public void addLink( final NetworkLink link ) throws SimulatorException
    {
        if (!nodes.containsKey( link.getSourceId() ))
        	throw new SimulatorException( "Node '" + link.getSourceId() + "' not found." );
        if (!nodes.containsKey( link.getDestId() ))
        	throw new SimulatorException( "Node '" + link.getDestId() + "' not found." );
        
    	List<NetworkLink> sLinks = links.get( link.getSourceId() );
        if(sLinks == null) sLinks = new ArrayList<NetworkLink>();
        sLinks.add( link );
    	links.put( link.getSourceId(), sLinks );
    	link.toString();
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
        addNode( new NetworkNode( id, name, delay, xPos, yPos ) );
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
    
    public NetworkNode nextNode( final long sourceId, final long destId )
    {
    	//System.out.println( "COMPUTING NEXT NODE FROM: " + sourceId );
    	NetworkNode[] predecessors = GraphPath.getShortestPath( sourceId, nodes, links );
    	// Just return the second node present in the list (if present).
    	NetworkNode currNode = nodes.get( destId ), nextNode = null;
    	//System.out.println( "Scanning list from: " + destId + ", source: " + sourceId );
    	while (predecessors[currNode.getIndex()] != null) {
    		nextNode = predecessors[currNode.getIndex()];
    		//System.out.println( "PRED: " + currNode.getId() + " = " + nextNode.getId() );
    		if (nextNode.getId() == sourceId) {
    			break;
    		} else {
    			currNode = nextNode;
    		}
    	}
    	return currNode;
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