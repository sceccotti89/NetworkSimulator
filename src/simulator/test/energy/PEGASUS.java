
package simulator.test.energy;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.utils.Time;

public class PEGASUS
{
    /*private List<Time> queries;
    private List<EnergyCPU> nodes;
    
    private long target;
    private static final Time WINDOW = new Time( 30, TimeUnit.SECONDS );
    
    private int size = 0;
    private long average = 0;
    
    private Time holdingTime;
    private boolean power_holding = false;
    private static final Time HOLD_TIME = new Time( 5, TimeUnit.MINUTES );*/
    
    private Map<Long,NodeInfo> _nodes;
    
    public PEGASUS( List<EnergyCPU> nodes, long target )
    {
        //this.nodes = nodes;
        //this.target = target;
        //queries = new LinkedList<>();
        
        long index = 0;
        _nodes = new HashMap<>( nodes.size() );
        for (EnergyCPU node : nodes) {
            _nodes.put( index++, new NodeInfo( node, target ) );
        }
    }
    
    public void setCompletedQuery( Time now, long nodeId, Time completionTime )
    {
        _nodes.get( nodeId ).setCompletedQuery( now, completionTime );
        
        /*final Time lowerBound = now.clone().subTime( WINDOW );
        while (size > 0) {
            Time time = queries.get( 0 );
            if (time.compareTo( lowerBound ) >= 0) {
                break;
            } else {
                queries.remove( 0 );
                if (size == 1) {
                    average = 0;
                } else {
                    average = ((average * size) - time.getTimeMicros()) / (size - 1);
                }
                size--;
            }
        }
        queries.add( completionTime );
        size++;
        
        if (power_holding) {
            if (now.clone().subTime( holdingTime ).compareTo( HOLD_TIME ) >= 0) {
                power_holding = false;
            } else {
                return;
            }
        }
        
        final long instantaneous = completionTime.getTimeMicros();
        average = average + ((instantaneous - average) / size);
        //final long average = computeAverage();
        if (average > target) {
            // Set max power, WAIT 5 minutes.
            for (EnergyCPU node : nodes) {
                node.setFrequency( now, node.getMaxFrequency() );
            }
            holdingTime = now.clone();
            power_holding = true;
        } else if (instantaneous > 1.35d * target) {
            // Set max power.
            for (EnergyCPU node : nodes) {
                node.setFrequency( now, node.getMaxFrequency() );
            }
        } else if (instantaneous > target) {
            // Increase power by 7% => increase frequency by 3.
            for (EnergyCPU node : nodes) {
                node.increaseFrequency( now, 3 );
            }
        } else if(instantaneous >= 0.85 * target && instantaneous <= target) {
            // Keep current power.
        } else if (instantaneous < 0.60 * target) {
            // Lower power by 3% => decrease frequency by 2.
            for (EnergyCPU node : nodes) {
                node.decreaseFrequency( now, 2 );
            }
        } else if (instantaneous < 0.85 * target) {
            // Lower power by 1% => decrease frequency by 1.
            for (EnergyCPU node : nodes) {
                node.decreaseFrequency( now, 1 );
            }
        }*/
    }
    
    private static class NodeInfo
    {
        private EnergyCPU node;
        
        private List<Time> queries;
        
        private long target;
        private static final Time WINDOW = new Time( 30, TimeUnit.SECONDS );
        
        private int size = 0;
        private long average = 0;
        
        private Time holdingTime;
        private boolean power_holding = false;
        private static final Time HOLD_TIME = new Time( 5, TimeUnit.MINUTES );
        
        public NodeInfo( EnergyCPU node, long target )
        {
            this.node = node;
            this.target = target;
            queries = new LinkedList<>();
        }
        
        public void setCompletedQuery( Time now, Time completionTime )
        {
            final Time lowerBound = now.clone().subTime( WINDOW );
            while (size > 0) {
                Time time = queries.get( 0 );
                if (time.compareTo( lowerBound ) >= 0) {
                    break;
                } else {
                    queries.remove( 0 );
                    if (size == 1) {
                        average = 0;
                    } else {
                        average = ((average * size) - time.getTimeMicros()) / (size - 1);
                    }
                    size--;
                }
            }
            queries.add( completionTime );
            size++;
            
            if (power_holding) {
                if (now.clone().subTime( holdingTime ).compareTo( HOLD_TIME ) >= 0) {
                    power_holding = false;
                } else {
                    return;
                }
            }
            
            final long instantaneous = completionTime.getTimeMicros();
            average = average + ((instantaneous - average) / size);
            if (average > target) {
                // Set max power, WAIT 5 minutes.
                node.setFrequency( now, node.getMaxFrequency() );
                holdingTime = now.clone();
                power_holding = true;
            } else if (instantaneous > 1.35d * target) {
                // Set max power.
                node.setFrequency( now, node.getMaxFrequency() );
            } else if (instantaneous > target) {
                // Increase power by 7% => increase frequency by 3.
                node.increaseFrequency( now, 3 );
            } else if(instantaneous >= 0.85 * target && instantaneous <= target) {
                // Keep current power.
            } else if (instantaneous < 0.60 * target) {
                // Lower power by 3% => decrease frequency by 2.
                node.decreaseFrequency( now, 2 );
            } else if (instantaneous < 0.85 * target) {
                // Lower power by 1% => decrease frequency by 1.
                node.decreaseFrequency( now, 1 );
            }
        }
    }
}
