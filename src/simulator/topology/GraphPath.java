/**
 * @author Stefano Ceccotti
*/

package simulator.topology;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

@Deprecated
public class GraphPath
{
    /* Map of {nodeID, [predecessors]} */
    private Map<Long,NetworkNode[]> shortestPaths = new HashMap<>( 1 << 7 );
    
    public GraphPath() {
        // Empty constructor.
    }
    
    public void computeAllSourcesShortestPath( NetworkTopology net,
                                               Map<Long,NetworkNode> nodes,
                                               Map<Long,List<NetworkLink>> links )
    {
        for (NetworkNode node : nodes.values()) {
            NetworkNode[] pred = getShortestPath( node.getId(), net, nodes, links );
            shortestPaths.put( node.getId(), pred );
        }
    }
    
    public NetworkNode[] getShortestPath( long sourceId,
                                          NetworkTopology net,
                                          Map<Long,NetworkNode> nodes,
                                          Map<Long,List<NetworkLink>> links )
    {
        NetworkNode[] pred = shortestPaths.get( sourceId );
        if (pred != null) {
            return pred;
        } else {
            pred = computeShortestPath( sourceId, net, nodes, links );
            shortestPaths.put( sourceId, pred );
            return pred;
        }
    }
    
    private NetworkNode[] computeShortestPath( long sourceId,
                                               NetworkTopology net,
                                               Map<Long,NetworkNode> nodes,
                                               Map<Long,List<NetworkLink>> links )
    {
        int size = nodes.size();
        Map<Long, List<QueueNode>> neighbours = new HashMap<>( size );
        NetworkNode[] predecessors = new NetworkNode[size];
        // Keep track of the nodes already inserted into the queue.
        Map<Long,QueueNode> addedNodes = new HashMap<Long,QueueNode>( size );
        Queue<QueueNode> queue = new PriorityQueue<>( size );
        for (NetworkNode node: nodes.values()) {
            if (node.isActive()) {
                List<NetworkLink> nLinks = links.get( node.getId() );
                if (nLinks != null) {
                    List<QueueNode> node_neighbours = new ArrayList<>( nLinks.size() );
                    for (NetworkLink link : nLinks) {
                        if (link.isActive()) {
                            NetworkNode destNode = nodes.get( link.getDestId() );
                            QueueNode qNode = addedNodes.get( destNode.getId() );
                            if(qNode == null) {
                                qNode = new QueueNode( destNode );
                                addedNodes.put( destNode.getId(), qNode );
                            }
                            node_neighbours.add( qNode );
                        }
                    }
                    neighbours.put( node.getId(), node_neighbours );
                }
                
                QueueNode qNode = addedNodes.get( sourceId );
                if(qNode == null) {
                    qNode = new QueueNode( node );
                    addedNodes.put( node.getId(), qNode );
                }
                qNode.setDistance( sourceId );
                queue.add( qNode );
            }
        }
        
        while (!queue.isEmpty()) {
            QueueNode n = queue.poll();
            if (n.isInfinite())
                break; // All remaining nodes are not accessible from the source.
            
            List<QueueNode> node_neighbours = neighbours.get( n._id );
            if (node_neighbours != null) {
                for (QueueNode node : node_neighbours) {
                    double dist = Math.max( n._distance, n._distance + net.getLink( n._id, node._id ).getTprop() );
                    if (dist < node._distance) {
                        node._distance = dist;
                        predecessors[node._index] = nodes.get( n._id );
                        // Reorder the queue.
                        queue.remove( node );
                        queue.add( node );
                    }
                }
            }
        }
        
        return predecessors;
    }
    
    private static class QueueNode implements Comparable<QueueNode>
    {
        private Double _distance;
        private long _id;
        private int _index;
        
        private static final double INFINITE = Double.MAX_VALUE;
        
        public QueueNode( NetworkNode node )
        { this( node.getId(), INFINITE, node.getIndex() ); }
        
        public QueueNode( long id, double distance, int index )
        {
            _id = id;
            _distance = distance;
            _index = index;
        }
        
        public void setDistance( long sourceId ) {
            _distance = (_id == sourceId) ? 0L : INFINITE;
        }
        
        public boolean isInfinite()
        { return _distance == INFINITE; }
        
        @Override
        public boolean equals( Object e ) {
            return ((QueueNode) e)._id == _id;
        }
        
        @Override
        public int compareTo( QueueNode node ) {
            return _distance.compareTo( node._distance );
        }
    }
}
