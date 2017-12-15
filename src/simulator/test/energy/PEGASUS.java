
package simulator.test.energy;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.utils.Pair;
import simulator.utils.Time;

public class PEGASUS
{
    private List<CPU> nodes;
    private List<Pair<Time,Time>> queries;
        
    private static final Time WINDOW = new Time( 30, TimeUnit.SECONDS );
    
    private double size = 0;
    private double average = 0;
    
    private Time holdingTime;
    private boolean power_holding = false;
    private static final Time HOLD_TIME = new Time( 5, TimeUnit.MINUTES );
    
    //private Map<Long,NodeInfo> _nodes;
    private long target;
    
    public PEGASUS( long target, List<CPU> nodes )
    {
        this.target = target;
        //_nodes = new HashMap<>();
        this.nodes = nodes;
        queries = new LinkedList<>();
    }
    
    /*public void addNode( long nodeId, CPU node ) {
        _nodes.put( nodeId, new NodeInfo( node, _target ) );
    }*/
    
    public void setCompletedQuery( Time now, long nodeId, Time completionTime )
    {
        //_nodes.get( nodeId ).setCompletedQuery( now, completionTime );
        
        final Time lowerBound = now.clone().subTime( WINDOW );
        while (size > 0) {
            Pair<Time,Time> query = queries.get( 0 );
            Time time = query.getFirst();
            if (time.compareTo( lowerBound ) >= 0) {
                break;
            } else {
                queries.remove( 0 );
                if (size == 1) {
                    average = 0;
                } else {
                    long completion = query.getSecond().getTimeMicros();
                    average = ((average * size) - completion) / (size - 1);
                }
                size--;
            }
        }
        queries.add( new Pair<>( now, completionTime ) );
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
            for (CPU node : nodes) {
                node.setPower( now, node.getMaxPower() );
            }
            holdingTime = now.clone();
            power_holding = true;
        } else if (instantaneous > 1.35d * target) {
            // Set max power.
            for (CPU node : nodes) {
                node.setPower( now, node.getMaxPower() );
            }
        } else if (instantaneous > target) {
            // Increase power by 7%.
            for (CPU node : nodes) {
                node.setPower( now, node.getPower() + node.getPower() * 0.07 );
            }
        } else if(instantaneous >= 0.85 * target && instantaneous <= target) {
            // Keep current power.
        } else if (instantaneous < 0.60 * target) {
            // Lower power by 3%.
            for (CPU node : nodes) {
                node.setPower( now, node.getPower() - node.getPower() * 0.03 );
            }
        } else if (instantaneous < 0.85 * target) {
            // Lower power by 1%.
            for (CPU node : nodes) {
                node.setPower( now, node.getPower() - node.getPower() * 0.01 );
            }
        }
    }
    
    /*private static class NodeInfo
    {
        private CPU node;
        
        private List<Pair<Time,Time>> queries;
        
        private long target;
        private static final Time WINDOW = new Time( 30, TimeUnit.SECONDS );
        
        private double size = 0;
        private double average = 0;
        
        private Time holdingTime;
        private boolean power_holding = false;
        private static final Time HOLD_TIME = new Time( 5, TimeUnit.MINUTES );
        
        public NodeInfo( CPU node, long target )
        {
            this.node = node;
            this.target = target;
            queries = new LinkedList<>();
        }
        
        public void setCompletedQuery( Time now, Time completionTime )
        {
            final Time lowerBound = now.clone().subTime( WINDOW );
            while (size > 0) {
                Pair<Time,Time> query = queries.get( 0 );
                Time time = query.getFirst();
                if (time.compareTo( lowerBound ) >= 0) {
                    break;
                } else {
                    queries.remove( 0 );
                    if (size == 1) {
                        average = 0;
                    } else {
                        long completion = query.getSecond().getTimeMicros();
                        average = ((average * size) - completion) / (size - 1);
                    }
                    size--;
                }
            }
            queries.add( new Pair<>( now, completionTime ) );
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
            //System.out.println( "ID: " + node.getAgent().getId() );
            //if (node.getAgent().getId() == 2)
            //    System.out.println( now + ": TARGET: " + target + ", AVG: " + average + ", INST: " + instantaneous );
            if (average > target) {
                // Set max power, WAIT 5 minutes.
                //node.setFrequency( now, node.getMaxFrequency() );
                node.setPower( now, node.getMaxPower() );
                holdingTime = now.clone();
                power_holding = true;
            } else if (instantaneous > 1.35d * target) {
                // Set max power.
                //node.setFrequency( now, node.getMaxFrequency() );
                node.setPower( now, node.getMaxPower() );
            } else if (instantaneous > target) {
                // Increase power by 7%.
                //node.increaseFrequency( now, 3 );
                node.setPower( now, node.getPower() + node.getPower() * 0.07 );
            } else if(instantaneous >= 0.85 * target && instantaneous <= target) {
                // Keep current power.
            } else if (instantaneous < 0.60 * target) {
                // Lower power by 3%.
                //node.decreaseFrequency( now, 2 );
                node.setPower( now, node.getPower() - node.getPower() * 0.03 );
            } else if (instantaneous < 0.85 * target) {
                // Lower power by 1%.
                //node.decreaseFrequency( now, 1 );
                node.setPower( now, node.getPower() - node.getPower() * 0.01 );
            }
        }
    }*/
}
