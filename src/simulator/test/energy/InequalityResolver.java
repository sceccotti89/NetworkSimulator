
package simulator.test.energy;

import java.io.IOException;
import java.io.PrintWriter;

public class InequalityResolver
{
    private static final double INCREMENT = 0.01d;
    private static final double OMEGA     = 4d;
    private static final double MAX_VALUE = 1d;
    
    private static final double TARGET_VALUE = 790400d;
    private static final double ERROR_PRECISION = 5d;
    private static final double ROUNDNESS = 100.0d;
    private static final double MIN = TARGET_VALUE - (TARGET_VALUE/ROUNDNESS*ERROR_PRECISION);
    private static final double MAX = TARGET_VALUE + (TARGET_VALUE/ROUNDNESS*ERROR_PRECISION);
    
    private static final int ITERATIONS = (int) (((1d - INCREMENT) * ROUNDNESS) *
                                                 ((1d - INCREMENT) * ROUNDNESS) *
                                                 ((1d - INCREMENT) * ROUNDNESS));
    
    public static void main( String[] argv ) throws IOException
    {
        double alpha = roundValue( INCREMENT );
        double beta  = roundValue( INCREMENT );
        double gamma = roundValue( INCREMENT );
        
        PrintWriter writer = new PrintWriter( "Results/Coefficients.txt", "UTF-8" );
        
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
            
            final double value = 990836d * (1d - (alpha + (2.7d * beta) + gamma * (OMEGA - 2)));
            if (value >= MIN && value <= MAX) {
                writer.println( alpha + " " + beta + " " + gamma );
            }
            
            alpha = roundValue( alpha + INCREMENT );
        }
        
        writer.close();
    }
    
    private static double roundValue( double value ) {
        return Math.round( value * 100.0 ) / 100.0;
    }
}
