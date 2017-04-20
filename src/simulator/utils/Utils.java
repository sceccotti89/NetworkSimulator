/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class Utils
{
    // One million.
    public static final long MILLION = 1000000;
    // The biggest long number is used to represent the "infinite" value.
    public static final long INFINITE = Long.MAX_VALUE;
    // The simulator log writer.
    public static final Logger LOGGER = Logger.getLogger( "Simulator" );
    // Folder used to store the images.
    public static final String IMAGES_DIR = "Results/Images/";
    
    // Samplings ID.
    public static final String ENERGY_SAMPLING = "EnergyConsumption";
    public static final String TAIL_LATENCY_SAMPLING = "TailLatency";
    
    
    
    public static long getTimeInMicroseconds( final double time, final TimeUnit tUnit )
    {
        if (time != Double.POSITIVE_INFINITY || time != Double.MAX_VALUE) {
            switch ( tUnit ) {
                case DAYS:         return (long) (time * 24 * 60 * 60 * 1000L * 1000L);
                case HOURS:        return (long) (time * 60 * 60 * 1000L * 1000L);
                case MINUTES:      return (long) (time * 60 * 1000L * 1000L);
                case SECONDS:      return (long) (time * 1000L * 1000L);
                case MILLISECONDS: return (long) (time * 1000L);
                case MICROSECONDS: return (long) (time);
                case NANOSECONDS:  return (long) (time / 1000L);
                default: break;
            }
        }
        
        return (long) time;
    }
    
    public static long getNormalizedTime( final TimeUnit tUnit )
    {
        switch ( tUnit ) {
            case DAYS:         return 24 * 60 * 60 * 1000L * 1000L;
            case HOURS:        return 60 * 60 * 1000L * 1000L;
            case MINUTES:      return 60 * 1000L * 1000L;
            case SECONDS:      return 1000L * 1000L;
            case MILLISECONDS: return 1000L;
            case MICROSECONDS: return 1;
            default: break;
        }
        
        return 1;
    }
    
    public static boolean existsFile( final String filename )
    {
        File file = new File( filename );
        return file.exists();
    }
    
    public static void checkFile( final String filename ) throws IOException
    {
        File file = new File( filename );
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        } else {
            checkDirectory( file.getParent() );
        }
    }
    
    public static void checkDirectory( final String filename )
    {
        File file = new File( filename );
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}