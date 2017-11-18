/**
 * @author Stefano Ceccotti
*/

package simulator.topology;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Supernode, typically used to configure
 * networks composed of Autonomous Systems, Datacenters, etc.</br>
 * To model quite complex networks inside each Supernode there's the possibility
 * to build a set of inner sub-nodes, each of which can be, in turn, another Supernode
 * or a simple {@linkplain NetworkNode}.
*/
public class NetworkSuperNode extends NetworkNode
{
    // List of nodes and supernodes.
    private List<NetworkNode> nodes;
    
    
    
    public NetworkSuperNode( NetworkTopology net, long id, String name, long delay ) {
        this( net, id, name, delay, 0, 0 );
    }
    
    public NetworkSuperNode( NetworkTopology net, long id, String name,
                             long delay, int xPos, int yPos )
    {
        super( net, id, name, delay );
        
        nodes = new ArrayList<>();
    }
    
    public void addNode( NetworkNode node ) {
        nodes.add( node );
    }
    
    public NetworkNode getNodes( int index ) {
        return nodes.get( index );
    }
    
    public List<NetworkNode> getNodes() {
        return nodes;
    }
    
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( "SuperNode: " + super.toString() );
        return buffer.toString();
    }
}
