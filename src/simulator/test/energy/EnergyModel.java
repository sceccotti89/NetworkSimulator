
package simulator.test.energy;

import java.util.HashMap;
import java.util.Map;

import simulator.utils.Time;
import simulator.utils.Utils;

public abstract class EnergyModel
{
    protected static double cores;
    
    /** Static power of all power consuming components (except the cores). */
    private static final double Ps = 0.9d;
    /** Static power of a core. */
    private static final double Pind = 0.1d;
    
    // TODO dai paper risulta che alfa sia compreso tra [2,3].
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
}
    
class QueryEnergyModel extends EnergyModel
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


/*class CoefficientEnergyModel extends EnergyModel
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

class CoefficientEnergyModel extends EnergyModel
{
    //      POWER           STDEV
    //16.8328547627930 1.30708580525330
    //14.4307527066090 1.09306474374930
    //12.6906984252240 0.95205071240478
    //11.5258791992070 0.88724265255830
    //10.3568317041610 0.78945596138226
    // 9.3216932801026 0.71063648095951
    // 7.5992422893111 0.59356681957165
    // 6.6835769518592 0.57524509807136
    // 6.2681729257489 0.52485317733415
    // 5.0965153747523 0.54660581478755
    // 4.1396654003963 0.46959844570467
    // 3.4359313987644 0.42770606537103
    // 3.0244325748922 0.34973582054577
    // 1.8851772840657 0.26837253660984
    // 1.3195298857676 0.09557728878342
    
    // TODO questi sono stati ricavati dai vecchi tempi
    //13.5217889497610 0.82879654866877
    //13.2819704394450 0.73959319778510
    //12.6050255857330 0.67142687240896
    //11.5347178342460 0.61465047452909
    //10.1841414628740 0.56368144284771
    // 8.6992531751952 0.51799071753261
    // 7.2158253910712 0.47597620681516
    // 6.5070780254109 0.45501747103376
    // 5.1959478995221 0.41063585066949
    // 4.0520239421844 0.36190673764664
    // 3.0812050099079 0.31051758681288
    // 2.2721971546800 0.26031102267178
    // 1.6057633127404 0.21622037891387
    // 1.0605248931111 0.18382528954768
    // 0.6160728234059 0.16819500218068

    
    private final Map<Long,Double> coefficients;
    
    public CoefficientEnergyModel( int nCores )
    {
        super( nCores );
        
        coefficients = new HashMap<>( 15 );
        coefficients.put(  800000L,  0.4195298857676 );
        coefficients.put( 1000000L,  0.9851772840657 );
        coefficients.put( 1200000L,  2.1244325748922 );
        coefficients.put( 1400000L,  2.5359313987644 );
        coefficients.put( 1600000L,  3.2396654003963 );
        coefficients.put( 1800000L,  4.1965153747523 );
        coefficients.put( 2000000L,  5.3681729257489 );
        coefficients.put( 2100000L,  5.7835769518592 );
        coefficients.put( 2300000L,  6.6992422893111 );
        coefficients.put( 2500000L,  8.4216932801026 );
        coefficients.put( 2700000L,  9.4568317041610 );
        coefficients.put( 2900000L, 10.6258791992070 );
        coefficients.put( 3100000L, 11.7906984252240 );
        coefficients.put( 3300000L, 13.5307527066090 );
        coefficients.put( 3500000L, 15.9328547627930 );
    }
    
    @Override
    public double computeEnergy( double energy, long frequency, Time interval, boolean idle ) {
        return computeEnergy( energy, frequency, interval.getTimeMicros(), idle );
    }
    
    @Override
    public double computeEnergy( double energy, long frequency, double interval, boolean idle )
    {
        double power = coefficients.get( frequency );
        return power * (interval / Utils.MILLION);
    }
    
    @Override
    public double getIdleEnergy( long frequency, Time interval ) {
        return getIdleEnergy( frequency, interval.getTimeMicros() );
    }
    
    @Override
    public double getIdleEnergy( long frequency, long interval ) {
        return getStaticPower( interval );
    }
}

class ParameterEnergyModel extends EnergyModel
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
