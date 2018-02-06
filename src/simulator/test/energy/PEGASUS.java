
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
    
    private long target;
    
    public PEGASUS( long target, List<CPU> nodes )
    {
        this.target = target;
        this.nodes = nodes;
        queries = new LinkedList<>();
    }
    
    public void setCompletedQuery( Time now, Time completionTime )
    {
        // TODO leggendo l'articolo sembrerebbe che l'aggiornamento non sia immediato
        // TODO ma dipenda da diversi fattori. Loro lo hanno calcolato in 3 secondi.
        
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
            if (now.compareTo( holdingTime ) > 0) {
                power_holding = false;
            } else {
                return;
            }
        }
        
        final long instantaneous = completionTime.getTimeMicros();
        average = average + ((instantaneous - average) / size);
        //System.out.println( "INST: " + instantaneous + ", AVG: " + average + ", TARGET: " + target );
        if (average > target) {
            // Set max power, WAIT 5 minutes.
            for (CPU node : nodes) {
                node.setPower( now, node.getMaxPower() );
            }
            //System.out.println( "SETTATO HOLDING TIME: " + now );
            holdingTime = now.clone().addTime( HOLD_TIME );
            power_holding = true;
        } else if (instantaneous > 1.35d * target) {
            // Set max power.
            for (CPU node : nodes) {
                node.setPower( now, node.getMaxPower() );
            }
            //System.out.println( "SETTATO MAX POWER: " + now );
        } else if (instantaneous > target) {
            // Increase power by 7%.
            for (CPU node : nodes) {
                node.setPower( now, node.getPower() + node.getPower() * 0.07 );
            }
            //System.out.println( "AUMENTA DEL 7%: " + now );
        } else if(instantaneous >= 0.85 * target && instantaneous <= target) {
            // Keep current power.
            //System.out.println( "INVARIATO: " + now );
        } else if (instantaneous < 0.60 * target) {
            // Lower power by 3%.
            for (CPU node : nodes) {
                node.setPower( now, node.getPower() - node.getPower() * 0.03 );
            }
            //System.out.println( "DIMINUISCI DEL 3%: " + now );
        } else if (instantaneous < 0.85 * target) {
            // Lower power by 1%.
            for (CPU node : nodes) {
                node.setPower( now, node.getPower() - node.getPower() * 0.01 );
            }
            //System.out.println( "DIMINUISCI DELL'1%: " + now );
        }
    }
}
