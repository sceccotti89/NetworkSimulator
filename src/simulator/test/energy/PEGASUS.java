
package simulator.test.energy;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.utils.Time;

public class PEGASUS
{
    private List<Time> queries;
    private List<EnergyCPU> nodes;
    
    private long target;
    private static final Time WINDOW = new Time( 30, TimeUnit.SECONDS );
    
    private int size = 0;
    private long average = 0;
    
    private Time holdingTime;
    private boolean power_holding = false;
    private static final Time HOLD_TIME = new Time( 5, TimeUnit.MINUTES );
    
    public PEGASUS( final List<EnergyCPU> nodes, final long target )
    {
        this.nodes = nodes;
        this.target = target;
        queries = new LinkedList<>();
    }
    
    public void setCompletedQuery( final Time now, final Time completionTime )
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
            // TODO Increase power by 7% => incrementare di 4 la frequenza?
            // TODO da qui in poi devo settare la frequenza per ogni core o di tutti quanti?
            // TODO Sembrerebbe che ogni core abbia la stessa "POTENZA",
            // TODO cio' non vuol dire che abbiano la stessa frequenza.
            for (EnergyCPU node : nodes) {
                node.setFrequency( now, node.getFrequency() );
            }
        } else if(instantaneous >= 0.85 * target && instantaneous <= target) {
            // Keep current power.
        } else if (instantaneous < 0.60 * target) {
            // TODO Lower power by 3% => decrementare di 2 la frequenza?
            for (EnergyCPU node : nodes) {
                node.setFrequency( now, node.getMaxFrequency() );
            }
        } else if (instantaneous < 0.85 * target) {
            // TODO Lower power by 1% => decrementare di 1 la frequenza?
            for (EnergyCPU node : nodes) {
                node.setFrequency( now, node.getMaxFrequency() );
            }
        }
    }
    
    /*private long computeAverage()
    {
        long average = 0;
        for (Time time : queries) {
            average += time.getTimeMicros();
        }
        return average / queries.size();
    }*/
}