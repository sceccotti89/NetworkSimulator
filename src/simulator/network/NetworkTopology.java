/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class NetworkTopology
{
    // TODO se questi devono essere acceduti conviene usare una struttura "piu' intelligente"
    private List<NetworkLink> networkLinks = new ArrayList<>( 32 );
    private List<NetworkNode> networkNodes = new ArrayList<>( 32 );
    
    public NetworkTopology()
    {
        
    }
    
    public NetworkTopology( final String filename ) throws IOException
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
    */
    private void build( final String filename ) throws IOException
    {
        BufferedReader br = new BufferedReader( new FileReader( filename ) );
        StringBuilder content = new StringBuilder( 512 );
        
        /** File structure:
         * 
         * nodes => [{[xPos],[yPos],id, name, delay}]
         * links => [{fromId, destId, bw, delay, type}]
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
            double delay    = link.getDouble( NetworkLink.DELAY );
            
            NetworkLink _link = new NetworkLink( fromId, destId, bandwith, delay );
            addLink( _link );
            _link.toString();
        }
        
        br.close();
    }
    
    public void addLink( final long fromId, final long destId,
                         final double bandwith, final double delay, final int type ) {
        addLink( new NetworkLink( fromId, destId, bandwith, delay, type ) );
    }
    
    public void addLink( final NetworkLink link ) {
        // TODO we must check if the source and dest ids are present in the nodes list??
        networkLinks.add( link );
    }
    
    public NetworkLink getLink( final long nodeId, final long nextNode ) {
     // TODO per adesso e' cosi' poi lo ricerca in modo corretto.
        return networkLinks.get( 0 );
    }
    
    public void addNode( final long id, final String name, final long delay, final int xPos, final int yPos ) {
        addNode( new NetworkNode( id, name, delay, xPos, yPos ) );
    }
    
    public void addNode( final NetworkNode node ) {
        networkNodes.add( node );
    }
    
    public NetworkNode getNode( final long nodeId ) {
        // TODO per adesso e' cosi' poi lo ricerca in modo corretto.
        return networkNodes.get( 0 );
    }
    
    public long nextNode( final long fromId ) {
        // TODO calcola il prossimo nodo nella rete a cui inivare l'evento => Dijkstra??
        return 1;
    }
    
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 512 );
        
        buffer.append( "Nodes information:\n" );
        for(NetworkNode node : networkNodes)
            buffer.append( node.toString() );
        
        buffer.append( "\nLinks information:\n" );
        for(NetworkLink link : networkLinks)
            buffer.append( link.toString() );
        
        return buffer.toString();
    }
}