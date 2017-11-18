
package simulator.test.energy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplicatedGraph
{
    private Map<Integer,Node> nodes;
    
    public ReplicatedGraph( int size ) {
        nodes = new HashMap<>( size * 2 );
    }
    
    public void addNode( int index, int queries )
    {
        Node node = new Node( queries );
        nodes.put( index, node );
    }
    
    public void addLink( int fromIndex, int destIndex, double weight )
    {
        Link link = new Link( destIndex, weight );
        nodes.get( fromIndex ).addLink( link );
    }
    
    public void computeMinimumPath()
    {
        // TODO completare
        
    }
    
    public static class Link
    {
        private final double weight;
        
        public Link( int destIndex, double weight ) {
            this.weight = weight;
        }
        
        public double getWeight() {
            return weight;
        }
    }
    
    public static class Node
    {
        private final int queries;
        
        private List<Link> links;
        
        public Node( int queries )
        {
            this.queries = queries;
            links = new ArrayList<>();
        }
        
        public void addLink( Link link ) {
            links.add( link );
        }
        
        public int getQueries() {
            return queries;
        }
    }
}