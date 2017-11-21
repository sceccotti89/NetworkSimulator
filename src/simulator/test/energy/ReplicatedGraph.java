
package simulator.test.energy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import simulator.test.energy.EnergyTestREPLICA_DIST.SwitchAgent;

public class ReplicatedGraph
{
    private Map<Integer,Node> nodes;
    private Node[] prev;
    private int replicas_per_nodes;
    
    private static final double INFINITE = Double.MAX_VALUE;
    
    public ReplicatedGraph( int size, int replicas_per_nodes )
    {
        nodes = new HashMap<>( size * 2 );
        this.replicas_per_nodes = replicas_per_nodes;
    }
    
    public void addNode( int index, int neighbours )
    {
        Node node = new Node( index, neighbours );
        nodes.put( index, node );
    }
    
    public void connectNodes( int fromNode, int destNode ) {
        nodes.get( fromNode ).connectTo( nodes.get( destNode ) );
    }
    
    public boolean hasNode( int index ) {
        return nodes.containsKey( index );
    }

    public void computeMinimumPath( SwitchAgent agent, int[] replicas )
    {
        //int[] replicas = new int[(nodes.size()-2)/replicas_per_node + 1];
        Queue<Node> queue = new PriorityQueue<>( nodes.size() );
        prev = new Node[nodes.size()];
        for (int i = 0; i < prev.length; i++) {
            prev[i] = null;
            
            // Put the node into the ordered queue.
            Node n = nodes.get( i );
            n.setWeight( INFINITE );
            queue.add( n );
        }
        
        // Source node is at distance 0.
        nodes.get( 0 ).setWeight( 0 );
        
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            if (node.getWeight() == INFINITE) {
                // Unreachable node: exit.
                break;
            }
            
            final int index = node.getIndex();
            if (index == nodes.size() - 1) {
                // Reached the destination node: exit.
                break;
            }
            
            final int slotIndex  = getSlotIndex( index );
            final double queries = node.getQueries();
            //node.setQueries( node.getQueries() + agent.incomingQueries( slotIndex ) );
            System.out.println( "ESTRATTO: " + index + ", QUERY: " + node.getQueries() + ", WEIGHT: " + node.getWeight() );
            for (Node n : node.getNeightbours()) {
                final int nodes = getReplicas( n.getIndex() );
                // Last "link" has the number of remaining queries.
                double weight = (slotIndex == replicas.length) ?
                                queries : node.getWeight() + agent.getWeight( queries, nodes, slotIndex );
                System.out.println( "VICINO: " + n.getIndex() + ", DISTANZA: " + n.getWeight() + ", PESO_ATTUALE: " + weight );
                if (weight < n.getWeight()) {
                    n.setWeight( weight );
                    prev[n.getIndex()] = node;
                    if (slotIndex < replicas.length) {
                        n.setQueries( agent.getNextQueries( queries, nodes, slotIndex ) );
                    }
                    
                    // Reorder the queue.
                    queue.remove( n );
                    queue.add( n );
                }
            }
        }
        
        // TODO rimuovere il path dopo i TEST
        Node currNode = nodes.get( nodes.size() - 1 );
        Node nextNode = null;
        int replicaIndex = replicas.length - 1;
        String path = currNode.getIndex() + "]";
        while ((nextNode = prev[currNode.getIndex()]) != null) {
            path = nextNode.getIndex() + "," + path;
            if (nextNode.getIndex() == 0) {
                break;
            } else {
                currNode = nextNode;
                // Assign the number of replicas.
                replicas[replicaIndex--] = getReplicas( nextNode.getIndex() );
            }
        }
        path = "[" + path;
        System.out.println( path );
        for (int i = 0; i < replicas.length; i++) {
            System.out.println( replicas[i] );
        }
    }
    
    private int getSlotIndex( double index ) {
        return (int) Math.ceil( index / 2 );
    }
    
    public int getReplicas( int index ) {
        return ((index - 1) % replicas_per_nodes) + 1;
    }
    
    @Override
    public String toString()
    {
        String content = "[";
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get( i );
            content += node;
            if (i < nodes.size() - 1) {
                content += ",\n";
            }
        }
        content += "]";
        return content;
    }
    
    public static class Node implements Comparable<Node>
    {
        private int index;
        private double queries;
        private Double _weight;
        
        private List<Node> _neighbours;
        
        public Node( int index, int neighbours )
        {
            _neighbours = new ArrayList<>( neighbours );
            this.index = index;
        }
        
        public int getIndex() {
            return index;
        }
        
        public void connectTo( Node dest ) {
            _neighbours.add( dest );
        }
        
        public List<Node> getNeightbours() {
            return _neighbours;
        }
        
        public void setQueries( double queries ) {
            this.queries = queries;
        }
        
        public void setWeight( double weight ) {
            _weight = weight;
        }
        
        public Double getWeight() {
            return _weight;
        }
        
        public double getQueries() {
            return queries;
        }
        
        @Override
        public int compareTo( Node node ) {
            return _weight.compareTo( node._weight );
        }
        
        @Override
        public String toString()
        {
            String content = "{" + index + ", Neighbours: [";
            for (int i = 0; i < _neighbours.size(); i++) {
                Node n = _neighbours.get( i );
                content += n.getIndex();
                if (i < _neighbours.size() - 1) {
                    content += ",";
                }
            }
            content += "]}";
            return content;
        }
    }
}