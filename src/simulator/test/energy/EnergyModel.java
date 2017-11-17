
package simulator.test.energy;

import simulator.utils.Time;
import simulator.utils.Utils;

public abstract class EnergyModel
{
    // TODO su internet ho trovato che il TDP in idle di un i7 4770k e' di 2W.
    private static final double ENERGY_IDLE = 1.3d / 4d;
    
    public EnergyModel()
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
    }
    
    /**
     * Computes the energy consumed in working phase.
     * 
     * @param energy       the initial energy consumption; it can be 0.
     * @param frequency    current frequency.
     * @param interval     interval of time to compute.
     * @param idle         {@code true} if the energy is computed for an idle period,
     *                     {@code false} otherwise.
    */
    public abstract double computeEnergy( final double energy, final long frequency, final Time interval, final boolean idle );
    
    /**
     * Computes the energy consumed in working phase.
     * 
     * @param energy       the initial energy consumption; it can be 0.
     * @param frequency    current frequency.
     * @param interval     interval of time to compute, expressed in ms.
     * @param idle         {@code true} if the energy is computed for an idle period,
     *                     {@code false} otherwise.
    */
    public abstract double computeEnergy( final double energy, final long frequency, final double interval, final boolean idle );
    
    /**
     * Computes the energy consumed in idle phase.
     * 
     * @param frequency    current frequency.
     * @param interval     interval of time to compute.
    */
    public abstract double getIdleEnergy( final long frequency, final Time interval );
    
    /**
     * Computes the energy consumed in idle phase.
     * 
     * @param frequency    current frequency.
     * @param interval     interval of time to compute, expressed in ms.
    */
    public abstract double getIdleEnergy( final long frequency, final long interval );
    
    
    
    public static class QueryEnergyModel extends EnergyModel
    {
        @Override
        public double computeEnergy( double energy, long frequency, Time interval, boolean idle) {
            return computeEnergy( energy, frequency, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( double energy, long frequency, double interval, boolean idle ) {
            return energy;
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final Time interval ) {
            return getIdleEnergy( 0L, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final long interval ) {
            return ENERGY_IDLE * (interval / Utils.MILLION);
        }
    }
    
    
    public static class NormalizedEnergyModel extends EnergyModel
    {
        @Override
        public double computeEnergy( final double energy, final long frequency, final Time interval, final boolean idle ) {
            return computeEnergy( energy, 0L, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( final double energy, final long frequency, final double interval, final boolean idle ) {
            return energy - (3d * ENERGY_IDLE * (interval/Utils.MILLION));
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final Time interval ) {
            return getIdleEnergy( 0L, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final long interval ) {
            return ENERGY_IDLE * (interval / Utils.MILLION);
        }
    }
    
    
    public static class CoefficientEnergyModel extends EnergyModel
    {
        private static final double[] COEFF = new double[]{ 1.29223297d, 0.94764905d };
        private static final double Pind = 0.1d;
        
        @Override
        public double computeEnergy( final double energy, final long frequency, final Time interval, final boolean idle ) {
            return computeEnergy( 0d, frequency, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( final double energy, final long frequency, final double interval, final boolean idle )
        {
            double freq = frequency / Utils.MILLION;
            double power = COEFF[1] - Pind;
            if (!idle) {
                power += COEFF[0] * (freq * freq) + Pind;
            }
            return power * (interval / Utils.MILLION);
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final Time interval ) {
            return getIdleEnergy( frequency, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final long interval ) {
            return computeEnergy( 0d, frequency, interval, true );
        }
    }
    
    public static class ParameterEnergyModel extends EnergyModel
    {
        private static final double alpha = 0.03d;
        private static final double beta  = 0.03d;
        private static final double gamma = 0.01d;
        private static final double omega = 4d;
        
        @Override
        public double computeEnergy( final double energy, final long frequency, final Time interval, final boolean idle ) {
            return computeEnergy( energy, frequency, interval.getTimeMicros(), idle );
        }
        
        @Override
        public double computeEnergy( final double energy, final long frequency, final double interval, final boolean idle )
        {
            double _frequency = frequency / Utils.MILLION; 
            final double r = alpha + beta * (_frequency - 0.8d) + gamma * (omega - 2);
            return energy * (1 - r);
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final Time interval ) {
            return getIdleEnergy( frequency, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final long interval ) {
            return computeEnergy( ENERGY_IDLE * (interval / Utils.MILLION), frequency, 0d, true );
        }
    }
    
    /*public static class MatteoEnergyModel extends EnergyModel
    {
        private static double[] a = { 0.00095,0.00126,0.00216,0.00261,0.00328,0.00413,0.00530,0.00584,0.00662,0.00830,0.00940,0.01055,0.01186,0.01363,0.01607 };
        private static long[] b   = {  800000,1000000,1200000,1400000,1600000,1800000,2000000,2100000,2300000,2500000,2700000,2900000,3100000,3300000,3500000 };
        
        @Override
        public double computeEnergy( final double energy, final long frequency, final Time interval, final boolean idle ) {
            return computeEnergy( 0d, frequency, interval.getTimeMicros(), idle ) ;
        }
        
        @Override
        public double computeEnergy( final double energy, final long frequency, final double interval, final boolean idle )
        {
            int index = Arrays.binarySearch( b, frequency );
            return a[index] * (interval/1000d);
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final Time interval ) {
            return getIdleEnergy( frequency, interval.getTimeMicros() );
        }
        
        @Override
        public double getIdleEnergy( final long frequency, final long interval ) {
            return ENERGY_IDLE * (interval / Utils.MILLION);
        }
    }*/
}