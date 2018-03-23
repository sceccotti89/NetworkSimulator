
package simulator.test.energy;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.events.Packet;
import simulator.utils.Pair;
import simulator.utils.SizeUnit;
import simulator.utils.Time;

public class PEGASUS
{
    private Agent node;
    
    private List<Agent> nodes;
    private List<Pair<Time,Time>> queries;
        
    private static final Time WINDOW = new Time( 30, TimeUnit.SECONDS );
    
    private double size = 0;
    private double average = 0;
    
    private Time holdingTime;
    private boolean power_holding = false;
    private static final Time HOLD_TIME = new Time( 5, TimeUnit.MINUTES );
    
    private long target;
    
    public PEGASUS( Agent node, long target )
    {
        this.node = node;
        this.target = target;
        nodes = new ArrayList<>();
        queries = new LinkedList<>();
    }
    
    public void connect( Agent destination ) {
        nodes.add( destination );
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
        size++;
        queries.add( new Pair<>( now, completionTime ) );
        
        final long instantaneous = completionTime.getTimeMicros();
        average = average + ((instantaneous - average) / size);
        //average = getAverage( lowerBound );
        
        if (power_holding) {
            if (now.compareTo( holdingTime ) > 0) {
                power_holding = false;
            } else {
                return;
            }
        }
        
        //System.out.println( "INST: " + instantaneous + ", AVG: " + average + ", TARGET: " + target );
        if (average > target) {
            // Set max power, WAIT 5 minutes.
            sendMessage( true, 0 );
            //System.out.println( "SETTATO HOLDING TIME: " + now );
            holdingTime = now.clone().addTime( HOLD_TIME );
            power_holding = true;
        } else if (instantaneous > 1.35d * target) {
            // Set max power.
            sendMessage( true, 0 );
            //System.out.println( "SETTATO MAX POWER: " + now );
        } else if (instantaneous > target) {
            // Increase power by 7%.
            sendMessage( false, +0.07 );
            //System.out.println( "AUMENTA DEL 7%: " + now );
        } else if(instantaneous >= 0.85 * target && instantaneous <= target) {
            // Keep current power.
            //System.out.println( "INVARIATO: " + now );
        } else if (instantaneous < 0.60 * target) {
            // Lower power by 3% (TODO trovare il valore migliore [1.5% - 3%]).
            sendMessage( false, -0.03 );
            //System.out.println( "DIMINUISCI DEL 3%: " + now );
        } else if (instantaneous < 0.85 * target) {
            // Lower power by 1%.
            sendMessage( false, -0.01 );
            //System.out.println( "DIMINUISCI DELL'1%: " + now );
        }
    }
    
    private void sendMessage( boolean maximum, double coefficient )
    {
        PEGASUSmessage message = new PEGASUSmessage( maximum, coefficient );
        Packet packet = new Packet( 20, SizeUnit.BYTE );
        packet.addContent( Global.PEGASUS_CONTROLLER, message );
        for (Agent destination : nodes) {
            node.sendMessage( destination, packet, true );
        }
    }
    
    protected double getAverage( Time lowerBound )
    {
        double size = 0;
        double completionTime = 0;
        for (int i = queries.size() - 1; i >= 0; i--) {
            Pair<Time,Time> query = queries.get( i );
            Time time = query.getFirst();
            if (time.compareTo( lowerBound ) >= 0) {
                completionTime += query.getSecond().getTimeMicros();
                size++;
            } else {
                queries.remove( i );
            }
        }
        
        return completionTime / size;
    }
    
    public static class PEGASUSmessage
    {
        private boolean maximum;
        private double coefficient;
        
        public PEGASUSmessage( boolean max, double coefficient )
        {
            this.maximum = max;
            this.coefficient = coefficient;
        }
        
        public double getCoefficient() {
            return coefficient;
        }
        
        public boolean isMaximum() {
            return maximum;
        }
    }
}
