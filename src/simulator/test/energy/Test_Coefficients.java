
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import simulator.utils.Utils;

public class Test_Coefficients
{
    private static final String FOLDER = "Results/Coefficients/";
    
    private static final double OMEGA = 4d;
    
    private static final double ERROR_PRECISION = 10d;
    private static final double ROUNDNESS = 100.0d;
    
    private static final double INCREMENT = 0.01d;
    private static final double MAX_VALUE = 1d;
    
    private static final double PERF_TARGET_VALUE = 790400d;
    private static final double PERF_SIMULATOR_VALUE = 992317d;
    
    private static final double MIN = PERF_TARGET_VALUE - (PERF_TARGET_VALUE/ROUNDNESS*ERROR_PRECISION);
    private static final double MAX = PERF_TARGET_VALUE + (PERF_TARGET_VALUE/ROUNDNESS*ERROR_PRECISION);
    
    private static final int ITERATIONS = (int) (((1d - INCREMENT) * ROUNDNESS) *
                                                 ((1d - INCREMENT) * ROUNDNESS) *
                                                 ((1d - INCREMENT) * ROUNDNESS));
    
    public static void main( String[] argv ) throws IOException
    {
        PERFcoefficients();
        PESOScoefficients( 500,  "TC", "mono", 601670d );
        PESOScoefficients( 500,  "EC", "mono", 531100d );
        PESOScoefficients( 1000, "TC", "mono", 443730d );
        PESOScoefficients( 1000, "EC", "mono", 412060d );
    }
    
    private static void PERFcoefficients() throws IOException
    {
        double alpha = roundValue( INCREMENT );
        double beta  = roundValue( INCREMENT );
        double gamma = roundValue( INCREMENT );
        
        PrintWriter writer = new PrintWriter( "Results/Coefficients/Final_Coefficients.txt", "UTF-8" );
        
        for (int i = 0; i < ITERATIONS; i++) {
            if (alpha == MAX_VALUE) {
                alpha = roundValue( INCREMENT );
                beta  = roundValue( beta + INCREMENT );
                if (beta == MAX_VALUE) {
                    beta  = roundValue( INCREMENT );
                    gamma = roundValue( gamma + INCREMENT );
                    if (gamma == MAX_VALUE) {
                        gamma = roundValue( INCREMENT );
                    }
                }
            }
            
            final double value = PERF_SIMULATOR_VALUE * (1d - (alpha + (2.7d * beta) + gamma * (OMEGA - 2)));
            if (value >= MIN && value <= MAX) {
                writer.println( alpha + " " + beta + " " + gamma );
            }
            
            alpha = roundValue( alpha + INCREMENT );
        }
        
        writer.close();
    }
    
    private static void PESOScoefficients( int latency, String mode, String type, double target_value ) throws IOException
    {
        final String file = "PESOS_" + latency + "_" + mode + "_" + type;
        System.out.println( "Analyzing: " + file );
        
        List<Long> freqList     = new ArrayList<>();
        List<Double> energyList = new ArrayList<>();
        BufferedReader reader = new BufferedReader( new FileReader( FOLDER + file + "_Freq_Energy.txt" ) );
        String line;
        double totEnergy = 0;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s+" );
            totEnergy += Double.parseDouble( values[1] );
            freqList.add( Long.parseLong( values[0] ) );
            energyList.add( Double.parseDouble( values[1] ) );
        }
        reader.close();
        System.out.println( "TOT: " + totEnergy );
        
        final double MIN = target_value - (target_value/ROUNDNESS*ERROR_PRECISION);
        final double MAX = target_value + (target_value/ROUNDNESS*ERROR_PRECISION);
        
        StringBuilder toWrite = new StringBuilder();
        reader = new BufferedReader( new FileReader( FOLDER + "Final_Coefficients.txt" ) );
        while ((line = reader.readLine()) != null) {
            String[] coefficients = line.split( "\\s+" );
            double alpha = roundValue( Double.parseDouble( coefficients[0] ) );
            double beta  = roundValue( Double.parseDouble( coefficients[1] ) );
            double gamma = roundValue( Double.parseDouble( coefficients[2] ) );
            
            double totalEnergy = 0;
            for(int i = 0; i < energyList.size(); i++) {
                totalEnergy += normalizeEnergy( energyList.get( i ), freqList.get( i ), alpha, beta, gamma );
            }
            
            if (totalEnergy >= MIN && totalEnergy <= MAX) {
                //System.out.println( "TOTAL_ENERGY: " + totalEnergy );
                toWrite.append( alpha + " " + beta + " " + gamma + "\n" );
            }
        }
        
        reader.close();
        PrintWriter writer = new PrintWriter( FOLDER + "Final_Coefficients.txt", "UTF-8" );
        writer.write( toWrite.toString() );
        writer.close();
    }
    
    private static double normalizeEnergy( double energy, double frequency, double alpha, double beta, double gamma )
    {
        double _frequency = frequency / Utils.MILLION; 
        final double r = alpha + beta * (_frequency - 0.8d) + gamma * (OMEGA - 2);
        return energy * (1 - r);
    }
    
    private static double roundValue( double value ) {
        return Math.round( value * ROUNDNESS ) / ROUNDNESS;
    }
}
