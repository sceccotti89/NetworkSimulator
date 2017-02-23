/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class GraphPath
{
	/*public static long findPath( final long sourceId )
	{
		// TODO calcola il prossimo nodo nella rete a cui inoltrare l'evento => Dijkstra??
		return 1;
	}*/
	
    public static NetworkNode[] getShortestPath( final long sourceId,
                                                 final Map<Long,NetworkNode> nodes,
                                                 final Map<Long,List<NetworkLink>> links )
	{
		int size = nodes.size();
		Map<Long, List<QueueNode>> neighbours = new HashMap<>( size );
		NetworkNode[] predecessors = new NetworkNode[size];
		Queue<QueueNode> queue = new PriorityQueue<>( size );
		for (NetworkNode node: nodes.values()) {
			List<NetworkLink> nLinks = links.get( node.getId() );
			if (nLinks != null) {
				List<QueueNode> node_neighbours = new ArrayList<>( nLinks.size() );
				for (NetworkLink link : nLinks) {
					NetworkNode destNode = nodes.get( link.getDestId() );
					node_neighbours.add( new QueueNode( destNode ) );
				}
				neighbours.put( node.getId(), node_neighbours );
			}
			queue.add( new QueueNode( node, sourceId ) );
			predecessors[node.getIndex()] = null;
		}
		
		while (!queue.isEmpty()) {
			QueueNode n = queue.poll();
			if (n.isInfinite())
				break; // All remaining nodes are non accessible from the source.
			
			//System.out.println( "NEIGHBOURS: " + n._id );
			List<QueueNode> node_neighbours = neighbours.get( n._id );
			if (node_neighbours != null) {
				for (QueueNode node : node_neighbours) {
					double dist = n._distance + getLink( links, n._id, node._id ).getDelay();
					if(dist < node._distance) {
						//System.out.println( "TROVATO: " + node._id );
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
	
	private static NetworkLink getLink( final Map<Long,List<NetworkLink>> links,
										final long sourceId,
										final long destId )
	{
    	for (NetworkLink link : links.get( sourceId )) {
    		if (link.getDestId() == destId)
    			return link;
    	}
    	return null;
    }
	
	private static class QueueNode implements Comparable<QueueNode>
	{
		private Double _distance;
		private long _id;
		private int _index;
		
		private static final double INFINITE = Double.MAX_VALUE;
		
		public QueueNode( final NetworkNode node )
		{ this( node.getId(), INFINITE, node.getIndex() ); }
		
		public QueueNode( final NetworkNode node, final long sourceId )
		{ this( node.getId(), (node.getId() == sourceId) ? 0L : INFINITE, node.getIndex() ); }
		
		public QueueNode( final long id, final double distance, final int index )
		{
			_id = id;
			_distance = distance;
			_index = index;
		}

		public boolean isInfinite()
		{ return _distance == INFINITE; }

		@Override
		public int compareTo( final QueueNode node )
		{ return _distance.compareTo( node._distance ); }
	}
}