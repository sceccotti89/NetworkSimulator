
package simulator.test.energy;

import simulator.utils.Time;
import simulator.utils.Utils;

public abstract class EnergyModel
{
    protected static double cores;
    
    /** Static power of all power consuming components (except the cores). */
    private static final double Ps = 0.84d; //1.3d;
    /** Static power of a core. */
    private static final double Pind = 0.1d;
    
    /** Alpha value used to evaluate the cpu power consumption. */
    private static final double alpha = 1.49;
    /** Beta value used to evaluate the cpu power consumption. */
    private static final double beta = 2.1;
    
    
    /**
     * Creates a new energy model.
     * 
     * @param _cores    number of cpu cores.
    */
    public EnergyModel( int _cores ) {
        cores = _cores;
    }
    
    /**
     * Returns the static power of all power consuming components (except the cores),
     * expressed in Joules.
    */
    public static double getStaticPower() {
        return Ps;
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
     * Returns the static power of a core, expressed in Joules.
    */
    public static double getCoreStaticPower() {
        return Pind;
    }
    
    /**
     * Returns the static power of a core, expressed in Joules.
     * 
     * @param interval    static power calculated for the given interval.
     *                    Interval must be expressed in microseconds.
    */
    public static double getCoreStaticPower( Time interval ) {
        return getCoreStaticPower( interval.getTimeMicros() );
    }
    
    /**
     * Returns the static power of a core, expressed in Joules.
     * 
     * @param interval    static power calculated for the given interval.
     *                    Interval must be expressed in microseconds.
    */
    public static double getCoreStaticPower( double interval ) {
        return (Pind / cores) * (interval / Utils.MILLION);
    }
    
    /***/
    public static double getAlpha() {
        return alpha;
    }
    
    /***/
    public static double getBeta() {
        return beta;
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
        public QueryEnergyModel( int nCores ) {
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
        public NormalizedEnergyModel( int nCores ) {
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
        
        public CoefficientEnergyModel( int nCores ) {
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
        
        public ParameterEnergyModel( int nCores ) {
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
