
package simulator.test.energy;

import simulator.utils.Time;
import simulator.utils.Utils;

public abstract class EnergyModel
{
    protected static double cores;
    
    /** Static power of all power consuming components (except the cores). */
    public static final double Ps = 1.19d; //1.3d;
    /** Static power of a core. */
    //private static final double Pind = 0.1d;
    
    /**
     * Creates a new energy model.
     * 
     * @param _cores    number of cpu cores.
    */
    public EnergyModel( double _cores )
    {
        /*energyIdleValues = new HashMap<>( 15 );
        energyIdleValues.put( 3500000L, 4.3519141137124d / 4d );
        energyIdleValues.put( 3300000L, 3.8033657190635d / 4d );
        energyIdleValues.put( 3100000L, 3.7768193645485d / 4d );
        energyIdleValues.put( 2900000L, 3.8801409698997d / 4d );
        energyIdleValues.put( 2700000L, 2.8367161204013d / 4d );
        energyIdleValues.put( 2500000L, 1.9872807357860d / 4d );
        energyIdleValues.put( 2300000L, 1.7678120066890d / 4d );
        energyIdleValues.put( 2100000L, 1.6599310033445d / 4d );
        energyIdleValues.put( 2000000L, 1.4050869230769d / 4d );
        energyIdleValues.put( 1800000L, 1.2006148494983d / 4d );
        energyIdleValues.put( 1600000L, 1.1307208361204d / 4d );
        energyIdleValues.put( 1400000L, 1.1299250167224d / 4d );
        energyIdleValues.put( 1200000L, 1.1167856856187d / 4d );
        energyIdleValues.put( 1000000L, 1.1034046822742d / 4d );
        energyIdleValues.put(  800000L, 1.1007310702341d / 4d );*/
        
        cores = _cores;
    }
    
    /**
     * Returns the static power of all power consuming components (except the cores),
     * expressed in Joules.
     * 
     * @param interval    static power calculated for the given interval.
    */
    public static double getStaticPower( Time interval ) {
        return getStaticPower( interval.getTimeMicros() );
    }
    
    /**
     * Returns the static power of all power consuming components (except the cores),
     * expressed in Joules.
     * 
     * @param interval    static power calculated for the given interval.
     *                    Interval must be expressed in microseconds.
    */
    public static double getStaticPower( double interval ) {
        return (Ps / cores) * (interval / Utils.MILLION);
    }
    
    /**
     * Computes the energy consumed in the working period.
     * 
     * @param energy       the current energy consumption; it can be 0.
     * @param frequency    current frequency.
     * @param interval     interval of time to compute.
     * @param idle         {@code true} if the energy is computed for an idle period,
     *                     {@code false} otherwise.
    */
    public abstract double computeEnergy( double energy, long frequency, Time interval, boolean idle );
    
    /**
     * Computes the energy consumed in working phase.
     * 
     * @param energy       the initial energy consumption; it can be 0.
     * @param frequency    current frequency.
     * @param interval     interval of time to compute, expressed in ms.
     * @param idle         {@code true} if the energy is computed for an idle period,
     *                     {@code false} otherwise.
    */
    public abstract double computeEnergy( double energy, long frequency, double interval, boolean idle );
    
    /**
     * Computes the energy consumed in the idle period.
     * 
     * @param frequency    current frequency.
     * @param interval     interval of time to compute.
    */
    public abstract double getIdleEnergy( long frequency, Time interval );
    
    /**
     * Computes the energy consumed in the idle period.
     * 
     * @param frequency    current frequency.
     * @param interval     interval of time to compute, expressed in ms.
    */
    public abstract double getIdleEnergy( long frequency, long interval );
    
    
    
    public static class QueryEnergyModel extends EnergyModel
    {
        public QueryEnergyModel( double nCores ) {
            super( nCores );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, Time interval, boolean idle) {
            return computeEnergy( energy, frequency, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, double interval, boolean idle ) {
            return energy;
        }
        
        @Override
        public double getIdleEnergy( long frequency, Time interval ) {
            return getIdleEnergy( 0L, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( long frequency, long interval ) {
            return getStaticPower( interval );
        }
    }
    
    
    /*public static class NormalizedEnergyModel extends EnergyModel
    {
        public NormalizedEnergyModel( double nCores ) {
            super( nCores );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, Time interval, boolean idle ) {
            return computeEnergy( energy, 0L, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, double interval, boolean idle ) {
            return energy - (3d * Pind * (interval/Utils.MILLION));
        }
        
        @Override
        public double getIdleEnergy( long frequency, Time interval ) {
            return getIdleEnergy( 0L, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( long frequency, long interval ) {
            return Pind * (interval / Utils.MILLION);
        }
    }
    
    
    public static class CoefficientEnergyModel extends EnergyModel
    {
        private static final double[] COEFF = new double[]{ 1.29223297d, 0.94764905d };
        //private static final double Pind = 0.1d;
        
        public CoefficientEnergyModel( double nCores ) {
            super( nCores );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, Time interval, boolean idle ) {
            return computeEnergy( 0d, frequency, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, double interval, boolean idle )
        {
            double freq = frequency / Utils.MILLION;
            double power = COEFF[1] - Pind;
            if (!idle) {
                power += COEFF[0] * (freq * freq);
            }
            return power * (interval / Utils.MILLION);
        }
        
        @Override
        public double getIdleEnergy( long frequency, Time interval ) {
            return getIdleEnergy( frequency, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( long frequency, long interval ) {
            return computeEnergy( 0d, frequency, interval, true );
        }
    }*/
    
    public static class ParameterEnergyModel extends EnergyModel
    {
        private static final double ALPHA = 0.01d;
        private static final double BETA  = 0.06d;
        private static final double GAMMA = 0.01d;
        private static final double OMEGA = 4d;
        
        public ParameterEnergyModel( double nCores ) {
            super( nCores );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, Time interval, boolean idle ) {
            return computeEnergy( energy, frequency, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, double interval, boolean idle )
        {
            double _frequency = frequency / Utils.MILLION; 
            final double r = ALPHA + BETA * (_frequency - 0.8d) + GAMMA * (OMEGA - 2);
            return energy * (1 - r);
        }
        
        @Override
        public double getIdleEnergy( long frequency, Time interval ) {
            return getIdleEnergy( frequency, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( long frequency, long interval ) {
            //return computeEnergy( getStaticPower( interval ), frequency, 0d, true );
            return getStaticPower( interval );
        }
    }
}
