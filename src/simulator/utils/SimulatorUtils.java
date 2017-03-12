/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.util.concurrent.TimeUnit;

public class SimulatorUtils
{
    public static final int MILLION = 1000000;
    
    public static final long INFINITE = Long.MAX_VALUE;
    
    public static long getTimeInMicroseconds( final double time, final TimeUnit tUnit )
    {
        switch( tUnit ) {
            case DAYS:         return (long) (time * 24 * 60 * 60 * 1000L * 1000L);
            case HOURS:        return (long) (time * 60 * 60 * 1000L * 1000L);
            case MINUTES:      return (long) (time * 60 * 1000L * 1000L);
            case SECONDS:      return (long) (time * 1000L * 1000L);
            case MILLISECONDS: return (long) (time * 1000L);
            case MICROSECONDS: return (long) (time);
            case NANOSECONDS:  return (long) (time / 1000L);
            default: break;
        }
        return (long) time;
    }
}