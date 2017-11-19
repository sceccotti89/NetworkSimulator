
package simulator.test.energy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class ReplicatedGraph
{
    private Map<Integer,Node> nodes;
    private Node[] prev;
    
    private static final double INFINITE = Double.MAX_VALUE;
    
    public ReplicatedGraph( int size ) {
        nodes = new HashMap<>( size * 2 );
    }
    
    public void addNode( int index, int queries )
    {
        Node node = new Node( index, queries );
        nodes.put( index, node );
    }
    
    public void connectNodes( int fromNode, int destNode ) {
        nodes.get( fromNode ).connectTo( nodes.get( destNode ) );
    }
    
    public boolean hasNode( int index ) {
        return nodes.containsKey( index );
    }

    public int[] computeMinimumPath( List<Double> meanArrivalTime, List<Double> meanCompletionTime )
    {
        Queue<Node> queue = new PriorityQueue<>( nodes.size() );
        prev = new Node[nodes.size()];
        for (int i = 0; i < prev.length; i++) {
            prev[i] = null;
            
            // Put the node into the ordered queue.
            Node n = nodes.get( i );
            n.setWeight( INFINITE );
            queue.add( n );
        }
        
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.getWeight() == INFINITE) {
                break;
            }
            
            if (node.getIndex() == nodes.size()) {
                break;
            }
            
            for (Node n : node.getNeightbours()) {
                double weight = 0;// TODO completare usando i parametri di potenza e latenza
                if (weight < n.getWeight()) {
                    n.setWeight( weight );
                    prev[n.getIndex()] = node;
                    
                    // Reorder the queue.
                    queue.remove( n );
                    queue.add( n );
                }
            }
        }
        
        int[] replicas = new int[nodes.size()];
        Node currNode = nodes.get( nodes.size() );
        Node nextNode = null;
        while (prev[currNode.getIndex()] != null) {
            nextNode = prev[currNode.getIndex()];
            // FIXME inserire il numero query del nodo
            replicas[nextNode.getIndex()] = 0;
            if (nextNode.getIndex() == 0) {
                break;
            } else {
                currNode = nextNode;
            }
        }
        
        return replicas;
    }
    
    public static class Node implements Comparable<Node>
    {
        private int index;
        private final int queries;
        private Double _weight;
        
        private List<Node> neighbours;
        
        public Node( int index, int queries )
        {
            this.index = index;
            this.queries = queries;
        }
        
        public int getIndex() {
            return index;
        }
        
        public void connectTo( Node dest ) {
            neighbours.add( dest );
        }
        
        public List<Node> getNeightbours() {
            return neighbours;
        }
        
        public void setWeight( double weight ) {
            _weight = weight;
        }
        
        public Double getWeight() {
            return _weight;
        }
        
        public int getQueries() {
            return queries;
        }
        
        @Override
        public int compareTo( Node node ) {
            return _weight.compareTo( node._weight );
        }
    }
}