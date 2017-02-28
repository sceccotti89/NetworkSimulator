/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.util.concurrent.TimeUnit;

public class SimulatorUtils
{
    public static final int MILLION = 1000000;
    
    public static final long INFINITE = Long.MAX_VALUE;
    
	public enum Size{ B, KB, MB, GB, TB };
	
	private static final long BYTE   = 8L;
	private static final long KBYTES = 1024L;
	private static final long MBYTES = 1024L * 1024L;
	private static final long GBYTES = 1024L * 1024L * 1024L;
	private static final long TBYTES = 1024L * 1024L * 1024L * 1024L;
	
	public static Size getTypeFromBit( final String type )
	{
		switch( type ) {
			case( "b" ):  return Size.B;
			case( "Kb" ): return Size.KB;
			case( "Mb" ): return Size.MB;
			case( "Gb" ): return Size.GB;
			case( "Tb" ): return Size.TB;
		}
		return Size.B;
	}
	
	public static Size getTypeFromByte( final String type )
	{
		switch( type ) {
			case( "B" ):  return Size.B;
			case( "KB" ): return Size.KB;
			case( "MB" ): return Size.MB;
			case( "GB" ): return Size.GB;
			case( "TB" ): return Size.TB;
		}
		return Size.B;
	}
	
	public static double getSizeInByte( final double pktSize, final Size sizeType )
	{
		switch( sizeType ) {
			case B : return pktSize;
			case KB: return pktSize * KBYTES;
			case MB: return pktSize * MBYTES;
			case GB: return pktSize * GBYTES;
			case TB: return pktSize * TBYTES;
		}
		return pktSize;
	}
	
	public static double getSizeInBit( final double pktSize, final Size sizeType )
	{
		switch( sizeType ) {
			case B : return pktSize;
			case KB: return pktSize * KBYTES;
			case MB: return pktSize * MBYTES;
			case GB: return pktSize * GBYTES;
			case TB: return pktSize * TBYTES;
		}
		return pktSize;
	}
	
	public static double getSizeInBitFromByte( final double pktSize, final Size sizeType ) {
		return getSizeInByte( pktSize, sizeType ) * BYTE;
	}
	
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